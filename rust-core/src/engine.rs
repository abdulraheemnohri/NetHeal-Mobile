use crate::firewall::Firewall;
use crate::analyzer::risk_score;
use crate::healer::Healer;

pub struct Engine {
    firewall: Firewall,
}

impl Engine {
    pub fn new() -> Self {
        Engine {
            firewall: Firewall::new(),
        }
    }

    pub fn process_request(&mut self, domain: &str, rate: u32, unknown: bool, burst: f32) -> bool {
        let score = risk_score(rate, unknown, burst);

        if score > 70 {
            self.firewall.block_domain(domain);
            return false;
        }

        self.firewall.analyze(domain, rate)
    }

    pub fn is_ip_blocked(&self, ip: &str) -> bool {
        self.firewall.is_ip_blocked(ip)
    }

    pub fn heal(&self) {
        Healer::repair_rules();
    }
}
