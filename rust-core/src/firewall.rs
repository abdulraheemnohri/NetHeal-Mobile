use std::collections::{HashSet, HashMap};
use crate::analyzer::analyze_threat;

pub struct Firewall {
    blocked_ips: HashSet<String>,
    blocked_domains: HashSet<String>,
    whitelisted_ips: HashSet<String>,
    whitelisted_domains: HashSet<String>,
    protected_apps: HashMap<String, u8>,
    total_scanned: u64,
    total_blocked: u64,
    military_mode: bool,
    lockdown_mode: bool,
    kill_switch: bool,
    protocol_stats: HashMap<u8, u64>,
    blocked_targets: HashMap<String, u64>,
}

impl Firewall {
    pub fn new() -> Self {
        let mut f = Firewall {
            blocked_ips: HashSet::new(),
            blocked_domains: HashSet::new(),
            whitelisted_ips: HashSet::new(),
            whitelisted_domains: HashSet::new(),
            protected_apps: HashMap::new(),
            total_scanned: 0,
            total_blocked: 0,
            military_mode: false,
            lockdown_mode: false,
            kill_switch: false,
            protocol_stats: HashMap::new(),
            blocked_targets: HashMap::new(),
        };
        f.load_defaults();
        f
    }

    fn load_defaults(&mut self) {
        let wl_ips = vec!["127.0.0.1", "10.0.0.2", "8.8.8.8", "1.1.1.1"];
        for ip in wl_ips { self.whitelisted_ips.insert(ip.to_string()); }
        let wl_domains = vec!["android.com", "google.com", "gstatic.com", "akamaized.net"];
        for d in wl_domains { self.whitelisted_domains.insert(d.to_string()); }
        let bl_ips = vec!["31.13.71.36", "142.250.190.46", "157.240.22.35", "52.2.144.185"];
        for ip in bl_ips { self.blocked_ips.insert(ip.to_string()); }
        let bl_domains = vec!["telemetry.os", "ads.service.net", "tracker.io", "doubleclick.net", "facebook.net"];
        for d in bl_domains { self.blocked_domains.insert(d.to_string()); }
    }

    pub fn force_block_stat(&mut self) {
        self.total_scanned += 1;
        self.total_blocked += 1;
    }

    pub fn set_lockdown(&mut self, enabled: bool) { self.lockdown_mode = enabled; }
    pub fn set_kill_switch(&mut self, enabled: bool) { self.kill_switch = enabled; }

    pub fn analyze_packet(&mut self, dst_ip: &str, protocol: u8, app_id: Option<&str>, domain: Option<&str>) -> bool {
        self.total_scanned += 1;
        *self.protocol_stats.entry(protocol).or_insert(0) += 1;

        if self.kill_switch { self.total_blocked += 1; return false; }
        if self.whitelisted_ips.contains(dst_ip) { return true; }
        if let Some(d) = domain { if self.whitelisted_domains.iter().any(|wl| d.contains(wl)) { return true; } }
        if self.lockdown_mode { self.total_blocked += 1; return false; }

        let mut block_reason = None;
        let report = analyze_threat(0, 0.0, domain);
        if report.risk_score > 75 { block_reason = Some("AI_THREAT"); }
        if let Some(d) = domain { if self.blocked_domains.iter().any(|bl| d.contains(bl)) { block_reason = Some("DOMAIN_BLOCK"); } }
        if self.blocked_ips.contains(dst_ip) { block_reason = Some("IP_BLOCK"); }
        if let Some(id) = app_id { if let Some(&state) = self.protected_apps.get(id) { if state == 2 { block_reason = Some("APP_ISOLATION"); } } }

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
    pub fn set_military_mode(&mut self, enabled: bool) { self.military_mode = enabled; }
    pub fn get_stats(&self) -> (u64, u64) { (self.total_scanned, self.total_blocked) }
    pub fn get_top_blocked(&self) -> Vec<(String, u64)> {
        let mut top: Vec<_> = self.blocked_targets.iter().map(|(k, v)| (k.clone(), *v)).collect();
        top.sort_by(|a, b| b.1.cmp(&a.1));
        top.into_iter().take(5).collect()
    }
    pub fn get_protocol_counts(&self) -> HashMap<u8, u64> { self.protocol_stats.clone() }
    pub fn reset_rules(&mut self) { self.blocked_ips.clear(); self.blocked_domains.clear(); self.whitelisted_ips.clear(); self.whitelisted_domains.clear(); self.protected_apps.clear(); self.load_defaults(); }
    pub fn reset_stats(&mut self) { self.total_scanned = 0; self.total_blocked = 0; self.protocol_stats.clear(); self.blocked_targets.clear(); }
}
