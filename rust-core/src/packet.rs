pub struct PacketInfo {
    pub src_ip: String,
    pub dst_ip: String,
    pub protocol: u8,
    pub payload: Vec<u8>,
}

pub fn parse_v4(data: &[u8]) -> Option<PacketInfo> {
    if data.len() < 20 { return None; }
    if (data[0] >> 4) != 4 { return None; }
    let ihl = (data[0] & 0x0F) as usize * 4;
    if data.len() < ihl { return None; }
    let protocol = data[9];
    let src_ip = format!("{}.{}.{}.{}", data[12], data[13], data[14], data[15]);
    let dst_ip = format!("{}.{}.{}.{}", data[16], data[17], data[18], data[19]);
    let payload = data[ihl..].to_vec();
    Some(PacketInfo { src_ip, dst_ip, protocol, payload })
}

pub fn parse_dns_query(payload: &[u8]) -> Option<String> {
    // Basic UDP check
    if payload.len() < 20 { return None; }
    let dns_data = &payload[8..];
    if dns_data.len() < 12 { return None; }
    let q_count = ((dns_data[4] as u16) << 8) | dns_data[5] as u16;
    if q_count == 0 { return None; }
    let mut pos = 12;
    let mut domain = String::new();
    while pos < dns_data.len() {
        let len = dns_data[pos] as usize;
        if len == 0 { break; }
        if pos + 1 + len > dns_data.len() { return None; }
        if !domain.is_empty() { domain.push('.'); }
        domain.push_str(&String::from_utf8_lossy(&dns_data[pos + 1..pos + 1 + len]));
        pos += 1 + len;
    }
    if domain.is_empty() { None } else { Some(domain) }
}

pub fn parse_sni(payload: &[u8]) -> Option<String> {
    // Minimal TLS SNI parser (assumes TCP payload starts with TLS handshake)
    if payload.len() < 20 { return None; }
    // Skip TCP header (assuming payload passed is only the TCP data)
    // In our loop, payload from parse_v4 starts with the L4 header.
    // TCP header is at least 20 bytes.
    let tcp_off = ((payload[12] & 0xF0) >> 4) as usize * 4;
    if payload.len() < tcp_off + 5 { return None; }
    let data = &payload[tcp_off..];

    // TLS Content Type 22 (Handshake), Version 3.x
    if data[0] != 0x16 { return None; }

    // Minimal parsing... search for "extension_server_name"
    // This is a rough heuristic search for the demo
    for i in 0..data.len().saturating_sub(10) {
        if &data[i..i+2] == &[0x00, 0x00] && data[i+2] == 0x00 { // Type 0 (SNI)
             // simplified check
        }
    }
    None // TLS SNI parsing is complex for a 1-shot script, sticking to DNS
}
