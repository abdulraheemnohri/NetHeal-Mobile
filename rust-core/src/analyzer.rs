pub fn risk_score(request_rate: u32, unknown_domain: bool, burst_ratio: f32) -> u8 {
    let mut score = 0;

    // Request rate scoring
    if request_rate > 200 {
        score += 60;
    } else if request_rate > 100 {
        score += 30;
    }

    // Domain status scoring
    if unknown_domain {
        score += 40;
    }

    // Burst behavior scoring (e.g. 10x normal rate in short period)
    if burst_ratio > 10.0 {
        score += 50;
    } else if burst_ratio > 5.0 {
        score += 25;
    }

    if score > 100 { 100 } else { score as u8 }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_risk_scoring() {
        assert_eq!(risk_score(50, false, 1.0), 0);
        assert_eq!(risk_score(250, false, 1.0), 60);
        assert_eq!(risk_score(50, true, 1.0), 40);
        assert_eq!(risk_score(50, false, 12.0), 50);
        assert_eq!(risk_score(250, true, 12.0), 100);
    }
}
