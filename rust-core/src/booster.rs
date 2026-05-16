use rand::Rng;

pub struct Booster {
    pub booster_active: bool,
    pub multipath_active: bool,
    pub obfuscation_active: bool,
}

impl Booster {
    pub fn new() -> Self {
        Booster {
            booster_active: false,
            multipath_active: false,
            obfuscation_active: false,
        }
    }

    pub fn optimize_packet(&self, data: &mut [u8]) {
        if !self.booster_active { return; }
        if data.len() > 33 && data[9] == 6 {
            let ihl = (data[0] & 0x0F) as usize * 4;
            if data.len() >= ihl + 14 {
                data[ihl + 13] |= 0x08;
            }
        }
        if self.obfuscation_active && data.len() > 20 {
            data[1] = data[1].wrapping_add(1);
        }
    }

    pub fn apply_privacy_shield(&self, data: &mut [u8]) {
        if data.len() < 20 { return; }
        let mut rng = rand::thread_rng();
        data[8] = 64 + rng.gen_range(0..64);
        if data[9] == 6 {
            let ihl = (data[0] & 0x0F) as usize * 4;
            if data.len() >= ihl + 16 {
                data[ihl + 14] = 0xFF;
                data[ihl + 15] = 0xFF;
            }
        }
    }

    pub fn handle_link_bonding(&self) -> u8 {
        if !self.multipath_active { return 0; }
        use std::time::{SystemTime, UNIX_EPOCH};
        let t = SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_millis();
        (t % 2) as u8
    }
}
