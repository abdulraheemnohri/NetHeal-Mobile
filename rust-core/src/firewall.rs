use std::collections::{HashSet, HashMap};
use std::time::{Instant, Duration};
use crate::analyzer::{analyze_threat, detect_anomalies};

pub struct AppTraffic {
    pub bytes_sent: u64,
    pub bytes_recv: u64,
    pub packets: u64,
}

pub struct RateLimiter {
    pub bytes_this_window: u64,
    pub last_window: Instant,
}

pub struct Firewall {
    blocked_ips: HashSet<String>,
    blocked_domains: HashSet<String>,
    whitelisted_ips: HashSet<String>,
    whitelisted_domains: HashSet<String>,
    kill_list: HashSet<String>,
    protected_apps: HashMap<String, u8>,
    app_traffic: HashMap<String, AppTraffic>,
    bandwidth_limits: HashMap<String, u64>, // Bytes per sec
    rate_limiters: HashMap<String, RateLimiter>,
    active_connections: HashMap<String, (String, Instant)>, // IP -> (AppID, LastSeen)
    total_scanned: u64,
    total_blocked: u64,
    security_level: u8,
    active_profile: String,
    protocol_stats: HashMap<u8, u64>,
    blocked_targets: HashMap<String, u64>,
    performance_mode: bool,

    geo_blocked_countries: HashSet<String>,
    blocked_ports: HashSet<u16>,
    protocol_blocks: HashSet<u8>,

    stealth_mode: bool,
    dns_hardening: bool,

    learning_mode: bool,
    observed_domains: HashSet<String>,

    last_gc: Instant,

    // NEW: Smart Cache
    cache: HashMap<String, (bool, Instant)>, // Target -> (Allowed, Expiry)
}

impl Firewall {
    pub fn new() -> Self {
        let mut f = Firewall {
            blocked_ips: HashSet::new(),
            blocked_domains: HashSet::new(),
            whitelisted_ips: HashSet::new(),
            whitelisted_domains: HashSet::new(),
            kill_list: HashSet::new(),
            protected_apps: HashMap::new(),
            app_traffic: HashMap::new(),
            bandwidth_limits: HashMap::new(),
            rate_limiters: HashMap::new(),
            active_connections: HashMap::new(),
            total_scanned: 0,
            total_blocked: 0,
            security_level: 0,
            active_profile: "Default".to_string(),
            protocol_stats: HashMap::new(),
            blocked_targets: HashMap::new(),
            performance_mode: false,
            geo_blocked_countries: HashSet::new(),
            blocked_ports: HashSet::new(),
            protocol_blocks: HashSet::new(),
            stealth_mode: false,
            dns_hardening: false,
            learning_mode: false,
            observed_domains: HashSet::new(),
            last_gc: Instant::now(),
            cache: HashMap::new(),
        };
        f.load_defaults();
        f
    }

    fn load_defaults(&mut self) {
        let wl_ips = vec!["127.0.0.1", "10.0.0.2", "8.8.8.8", "1.1.1.1"];
        for ip in wl_ips { self.whitelisted_ips.insert(ip.to_string()); }
        let wl_domains = vec!["android.com", "google.com", "gstatic.com", "onrender.com"];
        for d in wl_domains { self.whitelisted_domains.insert(d.to_string()); }
    }

    pub fn set_security_level(&mut self, level: u8) { self.security_level = level; }
    pub fn set_profile(&mut self, profile: &str) { self.active_profile = profile.to_string(); }
    pub fn set_performance_mode(&mut self, enabled: bool) { self.performance_mode = enabled; }
    pub fn set_stealth_mode(&mut self, enabled: bool) { self.stealth_mode = enabled; }
    pub fn set_dns_hardening(&mut self, enabled: bool) { self.dns_hardening = enabled; }
    pub fn set_learning_mode(&mut self, enabled: bool) { self.learning_mode = enabled; }

    pub fn add_geo_block(&mut self, country: String) { self.geo_blocked_countries.insert(country); }
    pub fn remove_geo_block(&mut self, country: String) { self.geo_blocked_countries.remove(&country); }
    pub fn add_port_block(&mut self, port: u16) { self.blocked_ports.insert(port); }
    pub fn remove_port_block(&mut self, port: u16) { self.blocked_ports.remove(&port); }

    pub fn analyze_packet(&mut self, data: &[u8], dst_ip: &str, protocol: u8, app_id: Option<&str>, domain: Option<&str>) -> bool {
        self.total_scanned += 1;
        *self.protocol_stats.entry(protocol).or_insert(0) += 1;

        if let Some(id) = app_id {
            self.active_connections.insert(dst_ip.to_string(), (id.to_string(), Instant::now()));
        }

        // Smart Cache Check
        let target_key = domain.unwrap_or(dst_ip);
        if let Some((allowed, expiry)) = self.cache.get(target_key) {
            if Instant::now() < *expiry {
                if !allowed { self.total_blocked += 1; }
                return *allowed;
            }
        }

        if self.last_gc.elapsed() > Duration::from_secs(300) { self.perform_gc(); }

        if self.learning_mode {
            if let Some(d) = domain { self.observed_domains.insert(d.to_string()); }
        }

        if self.security_level == 4 { self.total_blocked += 1; return false; }
        if self.stealth_mode && protocol == 1 { self.total_blocked += 1; return false; }
        if self.kill_list.contains(dst_ip) { self.total_blocked += 1; return false; }
        if self.whitelisted_ips.contains(dst_ip) { return true; }
        if let Some(d) = domain { if self.whitelisted_domains.iter().any(|wl| d.contains(wl)) { return true; } }
        if self.security_level == 3 { self.total_blocked += 1; return false; }

        let mut block_reason = None;

        if let Some(id) = app_id {
            if let Some(&limit) = self.bandwidth_limits.get(id) {
                let limiter = self.rate_limiters.entry(id.to_string()).or_insert(RateLimiter { bytes_this_window: 0, last_window: Instant::now() });
                if limiter.last_window.elapsed() > Duration::from_secs(1) {
                    limiter.bytes_this_window = 0;
                    limiter.last_window = Instant::now();
                }
                if limiter.bytes_this_window + (data.len() as u64) > limit {
                    block_reason = Some("RATE_LIMIT_EXCEEDED");
                } else {
                    limiter.bytes_this_window += data.len() as u64;
                }
            }
        }

        if block_reason.is_none() {
            let skip_dpi = self.performance_mode && self.total_scanned % 2 == 0;
            if !skip_dpi {
                if self.dns_hardening && protocol == 17 {
                     let ihl = (data[0] & 0x0F) as usize * 4;
                     if data.len() > ihl + 4 {
                         let d_port = ((data[ihl+2] as u16) << 8) | data[ihl+3] as u16;
                         if d_port == 53 { block_reason = Some("DNS_HARDENING_BLOCK"); }
                     }
                }
                if self.protocol_blocks.contains(&protocol) { block_reason = Some("PROTO_BLOCKED"); }
                let ihl = (data[0] & 0x0F) as usize * 4;
                if data.len() > ihl + 4 && protocol == 6 {
                    let d_port = ((data[ihl+2] as u16) << 8) | data[ihl+3] as u16;
                    if self.blocked_ports.contains(&d_port) { block_reason = Some("PORT_BLOCKED"); }
                }
                if let Some(id) = app_id {
                    if let Some(&state) = self.protected_apps.get(id) {
                        if state == 2 { block_reason = Some("APP_FIREWALL_BLOCK"); }
                    }
                }
                if let Some(d) = domain { if self.blocked_domains.iter().any(|bl| d.contains(bl)) { block_reason = Some("BLACKLIST_DOMAIN"); } }
                if self.blocked_ips.contains(dst_ip) { block_reason = Some("BLACKLIST_IP"); }

                if block_reason.is_none() {
                    if let Some((score, _)) = detect_anomalies(data) {
                        if score > 80 {
                            block_reason = Some("IDS_DETECTION");
                            if score >= 95 { self.kill_list.insert(dst_ip.to_string()); }
                        }
                    }
                    let report = analyze_threat(0, 0.0, domain);
                    if report.risk_score > 85 { block_reason = Some("AI_THREAT"); }
                }
            }
        }

        let result = block_reason.is_none();
        if !result {
            self.total_blocked += 1;
            let target = domain.unwrap_or(dst_ip).to_string();
            *self.blocked_targets.entry(target).or_insert(0) += 1;
        }

        // Cache result for 60s
        self.cache.insert(target_key.to_string(), (result, Instant::now() + Duration::from_secs(60)));

        result
    }

    fn perform_gc(&mut self) {
        let now = Instant::now();
        self.active_connections.retain(|_, (_, last_seen)| now.duration_since(*last_seen) < Duration::from_secs(3600));
        self.rate_limiters.retain(|_, limiter| limiter.last_window.elapsed() < Duration::from_secs(60));
        self.cache.retain(|_, (_, expiry)| now < *expiry);
        self.last_gc = now;
        println!("🧹 GC: Optimized kernel memory usage");
    }

    pub fn record_traffic(&mut self, app_id: &str, bytes: u64, is_sent: bool) {
        let entry = self.app_traffic.entry(app_id.to_string()).or_insert(AppTraffic { bytes_sent: 0, bytes_recv: 0, packets: 0 });
        entry.packets += 1;
        if is_sent { entry.bytes_sent += bytes; } else { entry.bytes_recv += bytes; }
    }

    pub fn set_app_bandwidth_limit(&mut self, app_id: &str, limit: u64) {
        if limit == 0 { self.bandwidth_limits.remove(app_id); }
        else { self.bandwidth_limits.insert(app_id.to_string(), limit); }
    }

    pub fn block_domain(&mut self, d: &str) { self.blocked_domains.insert(d.to_string()); }
    pub fn unblock_domain(&mut self, d: &str) { self.blocked_domains.remove(d); }
    pub fn block_ip(&mut self, ip: &str) { self.blocked_ips.insert(ip.to_string()); }
    pub fn unblock_ip(&mut self, ip: &str) { self.blocked_ips.remove(ip); }
    pub fn whitelist_ip(&mut self, ip: &str) { self.whitelisted_ips.insert(ip.to_string()); }
    pub fn unwhitelist_ip(&mut self, ip: &str) { self.whitelisted_ips.remove(ip); }
    pub fn whitelist_domain(&mut self, d: &str) { self.whitelisted_domains.insert(d.to_string()); }
    pub fn unwhitelist_domain(&mut self, d: &str) { self.whitelisted_domains.remove(d); }
    pub fn set_app_state(&mut self, app_id: &str, state: u8) { self.protected_apps.insert(app_id.to_string(), state); }
    pub fn kill_ip(&mut self, ip: &str) { self.kill_list.insert(ip.to_string()); self.active_connections.remove(ip); }

    pub fn get_stats(&self) -> (u64, u64) { (self.total_scanned, self.total_blocked) }
    pub fn get_app_usage_json(&self) -> String {
        let mut list = Vec::new();
        for (id, traffic) in &self.app_traffic {
            list.push(format!("\"{}\":{{\"s\":{},\"r\":{},\"p\":{}}}", id, traffic.bytes_sent, traffic.bytes_recv, traffic.packets));
        }
        format!("{{{}}}", list.join(","))
    }
    pub fn get_active_conns_json(&self) -> String {
        let mut list = Vec::new();
        for (target, (id, _)) in &self.active_connections { list.push(format!("{{\"t\":\"{}\",\"a\":\"{}\"}}", target, id)); }
        format!("[{}]", list.join(","))
    }
    pub fn get_top_blocked(&self) -> Vec<(String, u64)> {
        let mut top: Vec<_> = self.blocked_targets.iter().map(|(k, v)| (k.clone(), *v)).collect();
        top.sort_by(|a, b| b.1.cmp(&a.1));
        top.into_iter().take(10).collect()
    }
    pub fn get_protocol_counts(&self) -> HashMap<u8, u64> { self.protocol_stats.clone() }
    pub fn get_observed_count(&self) -> usize { self.observed_domains.len() }

    pub fn reset_rules(&mut self) {
        self.blocked_ips.clear(); self.blocked_domains.clear();
        self.whitelisted_ips.clear(); self.whitelisted_domains.clear();
        self.protected_apps.clear(); self.kill_list.clear();
        self.geo_blocked_countries.clear(); self.blocked_ports.clear(); self.protocol_blocks.clear();
        self.load_defaults();
    }
    pub fn reset_stats(&mut self) {
        self.total_scanned = 0; self.total_blocked = 0;
        self.protocol_stats.clear(); self.blocked_targets.clear(); self.app_traffic.clear(); self.active_connections.clear(); self.observed_domains.clear();
        self.cache.clear();
    }
}
