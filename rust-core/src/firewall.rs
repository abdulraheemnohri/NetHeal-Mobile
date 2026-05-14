use std::collections::HashSet;

pub struct Firewall {
    blocked_ips: HashSet<String>,
    whitelisted_ips: HashSet<String>,
    protected_apps: HashSet<String>,
    total_scanned: u64,
    total_blocked: u64,
    military_mode: bool,
}

impl Firewall {
    pub fn new() -> Self {
        let mut f = Firewall {
            blocked_ips: HashSet::new(),
            whitelisted_ips: HashSet::new(),
            protected_apps: HashSet::new(),
            total_scanned: 0,
            total_blocked: 0,
            military_mode: false,
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

    pub fn analyze_packet(&mut self, dst_ip: &str, app_id: Option<&str>) -> bool {
        self.total_scanned += 1;

        if self.whitelisted_ips.contains(dst_ip) {
            return true;
        }

        if self.military_mode {
             if dst_ip.starts_with("10.") || dst_ip.starts_with("192.168.") || dst_ip.starts_with("172.") {
                 self.total_blocked += 1;
                 return false;
             }
             if dst_ip.starts_with("104.") || dst_ip.starts_with("151.") {
                 self.total_blocked += 1;
                 return false;
             }
        }

        if let Some(id) = app_id {
            if self.protected_apps.contains(id) {
                self.total_blocked += 1;
                return false;
            }
        }

        if self.blocked_ips.contains(dst_ip) {
            self.total_blocked += 1;
            return false;
        }

        true
    }

    pub fn block_ip(&mut self, ip: &str) { self.blocked_ips.insert(ip.to_string()); }
    pub fn unblock_ip(&mut self, ip: &str) { self.blocked_ips.remove(ip); }
    pub fn whitelist_ip(&mut self, ip: &str) { self.whitelisted_ips.insert(ip.to_string()); }
    pub fn unwhitelist_ip(&mut self, ip: &str) { self.whitelisted_ips.remove(ip); }
    pub fn set_app_protection(&mut self, app_id: &str, enabled: bool) {
        if enabled { self.protected_apps.insert(app_id.to_string()); }
        else { self.protected_apps.remove(app_id); }
    }
    pub fn get_stats(&self) -> (u64, u64) { (self.total_scanned, self.total_blocked) }
    pub fn reset_rules(&mut self) {
        self.blocked_ips.clear();
        self.protected_apps.clear();
        self.load_defaults();
    }
}
