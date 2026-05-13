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
            self.firewall.block(domain);
            return false;
        }

        true
    }

    pub fn heal(&self) {
        Healer::repair_rules();
    }
}
