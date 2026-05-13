pub fn risk_score(request_rate: u32, unknown_domain: bool) -> u8 {
    let mut score = 0;

    if request_rate > 100 {
        score += 50;
    }

    if unknown_domain {
        score += 40;
    }

    score
}
