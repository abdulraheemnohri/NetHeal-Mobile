pub struct Booster {
    pub booster_active: bool,
    pub multipath_active: bool,
    counter: u64,
}

impl Booster {
    pub fn new() -> Self {
        Booster {
            booster_active: false,
            multipath_active: false,
            counter: 0,
        }
    }

    pub fn optimize_packet(&self, data: &mut [u8]) {
        if !self.booster_active { return; }

        // Internet Speed Boost: TCP PSH flag injection to force immediate delivery
        if data.len() > 20 {
            let protocol = data[9];
            if protocol == 6 { // TCP
                let ihl = (data[0] & 0x0F) as usize * 4;
                if data.len() > ihl + 13 {
                    data[ihl + 13] |= 0x08; // Set PUSH flag
                }
            }
        }
    }

    pub fn handle_link_bonding(&mut self) -> u8 {
        if !self.multipath_active { return 0; }
        self.counter += 1;
        (self.counter % 2) as u8 // 0: WiFi, 1: SIM
    }
}
