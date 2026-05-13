use std::collections::HashMap;

pub struct Firewall {
    blocked: HashMap<String, bool>,
}

impl Firewall {
    pub fn new() -> Self {
        Firewall {
            blocked: HashMap::new(),
        }
    }

    pub fn analyze(&mut self, domain: &str, requests: u32) -> bool {
        if requests > 80 {
            self.block(domain);
            return false;
        }
        true
    }

    pub fn block(&mut self, domain: &str) {
        println!("🚫 Blocking: {}", domain);
        self.blocked.insert(domain.to_string(), true);
    }
}
