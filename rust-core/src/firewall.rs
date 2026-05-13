use std::collections::HashSet;

pub struct Firewall {
    blocked_domains: HashSet<String>,
    blocked_ips: HashSet<String>,
}

impl Firewall {
    pub fn new() -> Self {
        Firewall {
            blocked_domains: HashSet::new(),
            blocked_ips: HashSet::new(),
        }
    }

    pub fn analyze(&mut self, domain: &str, requests: u32) -> bool {
        if requests > 200 {
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
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_firewall_domain() {
        let mut fw = Firewall::new();
        fw.block_domain("malware.com");
        assert!(!fw.analyze("malware.com", 10));
        assert!(fw.analyze("google.com", 10));
    }

    #[test]
    fn test_firewall_ip() {
        let mut fw = Firewall::new();
        fw.block_ip("1.2.3.4");
        assert!(fw.is_ip_blocked("1.2.3.4"));
        assert!(!fw.is_ip_blocked("8.8.8.8"));
    }
}
