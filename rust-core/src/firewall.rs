use std::collections::HashSet;

pub struct Firewall {
    blocked_domains: HashSet<String>,
    blocked_ips: HashSet<String>,
    whitelisted_domains: HashSet<String>,
    protected_apps: HashSet<String>,
    total_scanned: u64,
    total_blocked: u64,
}

impl Firewall {
    pub fn new() -> Self {
        Firewall {
            blocked_domains: HashSet::new(),
            blocked_ips: HashSet::new(),
            whitelisted_domains: HashSet::new(),
            protected_apps: HashSet::new(),
            total_scanned: 0,
            total_blocked: 0,
        }
    }

    pub fn analyze_connection(&mut self, target: &str, is_ip: bool, requests: u32, app_id: Option<&str>) -> bool {
        self.total_scanned += 1;

        if self.whitelisted_domains.contains(target) {
            return true;
        }

        if let Some(id) = app_id {
            if self.protected_apps.contains(id) {
                self.total_blocked += 1;
                return false;
            }
        }

        if requests > 1000 {
            if is_ip { self.block_ip(target); } else { self.block_domain(target); }
            self.total_blocked += 1;
            return false;
        }

        let is_blacklisted = if is_ip {
            self.blocked_ips.contains(target)
        } else {
            self.blocked_domains.contains(target)
        };

        if is_blacklisted {
            self.total_blocked += 1;
            false
        } else {
            true
        }
    }

    pub fn block_domain(&mut self, domain: &str) {
        self.blocked_domains.insert(domain.to_string());
    }

    pub fn block_ip(&mut self, ip: &str) {
        self.blocked_ips.insert(ip.to_string());
    }

    pub fn remove_domain_block(&mut self, domain: &str) {
        self.blocked_domains.remove(domain);
    }

    pub fn remove_ip_block(&mut self, ip: &str) {
        self.blocked_ips.remove(ip);
    }

    pub fn add_to_whitelist(&mut self, domain: &str) {
        self.whitelisted_domains.insert(domain.to_string());
    }

    pub fn remove_from_whitelist(&mut self, domain: &str) {
        self.whitelisted_domains.remove(domain);
    }

    pub fn set_app_protection(&mut self, app_id: &str, enabled: bool) {
        if enabled {
            self.protected_apps.insert(app_id.to_string());
        } else {
            self.protected_apps.remove(app_id);
        }
    }

    pub fn get_stats(&self) -> (u64, u64) {
        (self.total_scanned, self.total_blocked)
    }

    pub fn reset_rules(&mut self) {
        self.blocked_domains.clear();
        self.blocked_ips.clear();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_firewall_logic() {
        let mut fw = Firewall::new();
        fw.add_to_whitelist("google.com");
        assert!(fw.analyze_connection("google.com", false, 10, None));
        fw.remove_from_whitelist("google.com");
        // Not whitelisted anymore, but not blocked either
        assert!(fw.analyze_connection("google.com", false, 10, None));
    }
}
