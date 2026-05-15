use std::collections::{HashSet, HashMap};
use crate::analyzer::analyze_threat;

pub struct AppTraffic {
    pub bytes_sent: u64,
    pub bytes_recv: u64,
    pub packets: u64,
}

pub struct Firewall {
    blocked_ips: HashSet<String>,
    blocked_domains: HashSet<String>,
    blocked_regions: HashSet<String>,
    whitelisted_ips: HashSet<String>,
    whitelisted_domains: HashSet<String>,
    protected_apps: HashMap<String, u8>,
    app_traffic: HashMap<String, AppTraffic>,
    bandwidth_limits: HashMap<String, u64>,
    total_scanned: u64,
    total_blocked: u64,
    security_level: u8,
    protocol_stats: HashMap<u8, u64>,
    blocked_targets: HashMap<String, u64>,
}

impl Firewall {
    pub fn new() -> Self {
        let mut f = Firewall {
            blocked_ips: HashSet::new(),
            blocked_domains: HashSet::new(),
            blocked_regions: HashSet::new(),
            whitelisted_ips: HashSet::new(),
            whitelisted_domains: HashSet::new(),
            protected_apps: HashMap::new(),
            app_traffic: HashMap::new(),
            bandwidth_limits: HashMap::new(),
            total_scanned: 0,
            total_blocked: 0,
            security_level: 0,
            protocol_stats: HashMap::new(),
            blocked_targets: HashMap::new(),
        };
        f.load_defaults();
        f
    }

    fn load_defaults(&mut self) {
        let wl_ips = vec!["127.0.0.1", "10.0.0.2", "8.8.8.8", "1.1.1.1"];
        for ip in wl_ips { self.whitelisted_ips.insert(ip.to_string()); }
        let wl_domains = vec!["android.com", "google.com", "gstatic.com"];
        for d in wl_domains { self.whitelisted_domains.insert(d.to_string()); }
        let bl_ips = vec!["31.13.71.36", "142.250.190.46"];
        for ip in bl_ips { self.blocked_ips.insert(ip.to_string()); }
        let bl_domains = vec!["telemetry.os", "ads.service.net", "tracker.io"];
        for d in bl_domains { self.blocked_domains.insert(d.to_string()); }
    }

    pub fn set_security_level(&mut self, level: u8) { self.security_level = level; }
    pub fn force_block_stat(&mut self) { self.total_scanned += 1; self.total_blocked += 1; }
    pub fn set_app_bandwidth_limit(&mut self, app_id: &str, limit: u64) {
        if limit == 0 { self.bandwidth_limits.remove(app_id); }
        else { self.bandwidth_limits.insert(app_id.to_string(), limit); }
    }
    pub fn block_region(&mut self, region: &str) { self.blocked_regions.insert(region.to_string()); }
    pub fn unblock_region(&mut self, region: &str) { self.blocked_regions.remove(region); }

    pub fn record_traffic(&mut self, app_id: &str, bytes: u64, is_sent: bool) {
        let entry = self.app_traffic.entry(app_id.to_string()).or_insert(AppTraffic { bytes_sent: 0, bytes_recv: 0, packets: 0 });
        entry.packets += 1;
        if is_sent { entry.bytes_sent += bytes; } else { entry.bytes_recv += bytes; }
    }

    pub fn analyze_packet(&mut self, dst_ip: &str, protocol: u8, app_id: Option<&str>, domain: Option<&str>) -> bool {
        self.total_scanned += 1;
        *self.protocol_stats.entry(protocol).or_insert(0) += 1;

        if self.security_level == 4 { self.total_blocked += 1; return false; }
        if self.whitelisted_ips.contains(dst_ip) { return true; }
        if let Some(d) = domain { if self.whitelisted_domains.iter().any(|wl| d.contains(wl)) { return true; } }
        if self.security_level == 3 { self.total_blocked += 1; return false; }

        let mut block_reason = None;
        if dst_ip.starts_with("45.") && self.blocked_regions.contains("EAST") { block_reason = Some("GEO_BLOCK"); }
        if let Some(d) = domain {
            if self.blocked_domains.iter().any(|bl| d.contains(bl)) { block_reason = Some("DOMAIN_BLOCK"); }
            let report = analyze_threat(0, 0.0, Some(d));
            if report.risk_score > 80 { block_reason = Some("AI_THREAT"); }
        }
        if self.blocked_ips.contains(dst_ip) { block_reason = Some("IP_BLOCK"); }
        if let Some(id) = app_id {
            if let Some(&state) = self.protected_apps.get(id) {
                if state == 2 { block_reason = Some("APP_ISOLATION"); }
            }
        }

        if block_reason.is_some() {
            self.total_blocked += 1;
            *self.blocked_targets.entry(domain.unwrap_or(dst_ip).to_string()).or_insert(0) += 1;
            return false;
        }
        true
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
    pub fn get_stats(&self) -> (u64, u64) { (self.total_scanned, self.total_blocked) }
    pub fn get_app_usage_json(&self) -> String {
        let mut list = Vec::new();
        for (id, traffic) in &self.app_traffic { list.push(format!("\"{}\":{{\"s\":{},\"r\":{},\"p\":{}}}", id, traffic.bytes_sent, traffic.bytes_recv, traffic.packets)); }
        format!("{{{}}}", list.join(","))
    }
    pub fn get_top_blocked(&self) -> Vec<(String, u64)> {
        let mut top: Vec<_> = self.blocked_targets.iter().map(|(k, v)| (k.clone(), *v)).collect();
        top.sort_by(|a, b| b.1.cmp(&a.1));
        top.into_iter().take(10).collect()
    }
    pub fn get_protocol_counts(&self) -> HashMap<u8, u64> { self.protocol_stats.clone() }
    pub fn reset_rules(&mut self) { self.blocked_ips.clear(); self.blocked_domains.clear(); self.whitelisted_ips.clear(); self.whitelisted_domains.clear(); self.protected_apps.clear(); self.load_defaults(); }
    pub fn reset_stats(&mut self) { self.total_scanned = 0; self.total_blocked = 0; self.app_traffic.clear(); self.blocked_targets.clear(); self.protocol_stats.clear(); }
}
