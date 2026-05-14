use std::collections::{HashSet, HashMap};

pub struct Firewall {
    blocked_ips: HashSet<String>,
    whitelisted_ips: HashSet<String>,
    protected_apps: HashMap<String, u8>, // 0: Allowed, 1: WiFi-Only (Sim), 2: Blocked
    total_scanned: u64,
    total_blocked: u64,
    military_mode: bool,
    protocol_stats: HashMap<u8, u64>, // protocol -> count
    blocked_targets: HashMap<String, u64>, // IP -> count
}

impl Firewall {
    pub fn new() -> Self {
        let mut f = Firewall {
            blocked_ips: HashSet::new(),
            whitelisted_ips: HashSet::new(),
            protected_apps: HashMap::new(),
            total_scanned: 0,
            total_blocked: 0,
            military_mode: false,
            protocol_stats: HashMap::new(),
            blocked_targets: HashMap::new(),
        };
        f.load_defaults();
        f
    }

    fn load_defaults(&mut self) {
        let defaults = vec![
            "8.8.4.4", "31.13.71.36", "142.250.190.46", "157.240.22.35",
            "20.190.129.0", "52.114.0.0", "13.107.42.0", "185.199.108.0",
            "104.244.42.0", "199.16.156.0", "69.171.224.0", "66.220.144.0",
            "52.2.144.185", "54.225.143.125", "23.235.32.0", "104.16.0.0",
            "172.217.0.0", "216.58.192.0", "40.76.0.0", "52.142.0.0",
            "1.1.1.1", "1.0.0.1", "8.8.8.8", "9.9.9.9", "149.112.112.112",
        ];
        for ip in defaults {
            self.blocked_ips.insert(ip.to_string());
        }
    }

    pub fn set_military_mode(&mut self, enabled: bool) {
        self.military_mode = enabled;
    }

    pub fn analyze_packet(&mut self, dst_ip: &str, protocol: u8, app_id: Option<&str>) -> bool {
        self.total_scanned += 1;
        *self.protocol_stats.entry(protocol).or_insert(0) += 1;

        if self.whitelisted_ips.contains(dst_ip) {
            return true;
        }

        let mut should_block = false;

        if self.military_mode {
             if dst_ip.starts_with("10.") || dst_ip.starts_with("192.168.") || dst_ip.starts_with("172.") {
                 should_block = true;
             }
             if dst_ip.starts_with("104.") || dst_ip.starts_with("151.") {
                 should_block = true;
             }
        }

        if let Some(id) = app_id {
            if let Some(&state) = self.protected_apps.get(id) {
                if state == 2 { // Full Block
                    should_block = true;
                }
            }
        }

        if self.blocked_ips.contains(dst_ip) {
            should_block = true;
        }

        if should_block {
            self.total_blocked += 1;
            *self.blocked_targets.entry(dst_ip.to_string()).or_insert(0) += 1;
            return false;
        }

        true
    }

    pub fn block_ip(&mut self, ip: &str) { self.blocked_ips.insert(ip.to_string()); }
    pub fn unblock_ip(&mut self, ip: &str) { self.blocked_ips.remove(ip); }
    pub fn whitelist_ip(&mut self, ip: &str) { self.whitelisted_ips.insert(ip.to_string()); }
    pub fn unwhitelist_ip(&mut self, ip: &str) { self.whitelisted_ips.remove(ip); }

    pub fn set_app_state(&mut self, app_id: &str, state: u8) {
        self.protected_apps.insert(app_id.to_string(), state);
    }

    pub fn get_stats(&self) -> (u64, u64) { (self.total_scanned, self.total_blocked) }

    pub fn get_top_blocked(&self) -> Vec<(String, u64)> {
        let mut top: Vec<_> = self.blocked_targets.iter().map(|(k, v)| (k.clone(), *v)).collect();
        top.sort_by(|a, b| b.1.cmp(&a.1));
        top.into_iter().take(5).collect()
    }

    pub fn get_protocol_counts(&self) -> HashMap<u8, u64> {
        self.protocol_stats.clone()
    }

    pub fn reset_rules(&mut self) {
        self.blocked_ips.clear();
        self.protected_apps.clear();
        self.load_defaults();
    }

    pub fn reset_stats(&mut self) {
        self.total_scanned = 0;
        self.total_blocked = 0;
        self.protocol_stats.clear();
        self.blocked_targets.clear();
    }
}
