use crate::firewall::Firewall;
use crate::analyzer;
use crate::healer::Healer;

pub struct Engine {
    pub firewall: Firewall,
}

impl Engine {
    pub fn new() -> Self {
        Engine {
            firewall: Firewall::new(),
        }
    }

    pub fn process_request(&mut self, domain: &str, rate: u32, is_unknown: bool) -> bool {
        let score = analyzer::risk_score(rate, is_unknown);
        if score > 70 {
            self.firewall.block(domain);
            return false;
        }
        self.firewall.analyze(domain, rate)
    }

    pub fn heal(&self) {
        Healer::repair_rules();
    }
}
