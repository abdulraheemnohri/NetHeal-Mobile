use std::collections::HashSet;

pub struct Firewall {
    blocked_domains: HashSet<String>,
    blocked_ips: HashSet<String>,
    protected_apps: HashSet<String>,
}

impl Firewall {
    pub fn new() -> Self {
        Firewall {
            blocked_domains: HashSet::new(),
            blocked_ips: HashSet::new(),
            protected_apps: HashSet::new(),
        }
    }

    pub fn analyze(&mut self, domain: &str, requests: u32) -> bool {
        if requests > 500 {
            self.block_domain(domain);
            return false;
        }
        !self.blocked_domains.contains(domain)
    }

    pub fn block_domain(&mut self, domain: &str) {
        self.blocked_domains.insert(domain.to_string());
    }

    pub fn block_ip(&mut self, ip: &str) {
        self.blocked_ips.insert(ip.to_string());
    }

    pub fn is_ip_blocked(&self, ip: &str) -> bool {
        self.blocked_ips.contains(ip)
    }

    pub fn set_app_protection(&mut self, app_id: &str, enabled: bool) {
        if enabled {
            self.protected_apps.insert(app_id.to_string());
        } else {
            self.protected_apps.remove(app_id);
        }
    }

    pub fn is_app_protected(&self, app_id: &str) -> bool {
        self.protected_apps.contains(app_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_firewall_domain() {
        let mut fw = Firewall::new();
        fw.block_domain("malware.com");
        assert!(!fw.analyze("malware.com", 10));
    }

    #[test]
    fn test_app_protection() {
        let mut fw = Firewall::new();
        fw.set_app_protection("com.chrome", true);
        assert!(fw.is_app_protected("com.chrome"));
        fw.set_app_protection("com.chrome", false);
        assert!(!fw.is_app_protected("com.chrome"));
    }
}
