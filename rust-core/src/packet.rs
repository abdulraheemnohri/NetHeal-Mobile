pub struct PacketInfo {
    pub src_ip: String,
    pub dst_ip: String,
    pub protocol: u8,
}

pub fn parse_v4(data: &[u8]) -> Option<PacketInfo> {
    if data.len() < 20 {
        return None;
    }

    // Check version (4)
    if (data[0] >> 4) != 4 {
        return None;
    }

    let protocol = data[9];
    let src_ip = format!("{}.{}.{}.{}", data[12], data[13], data[14], data[15]);
    let dst_ip = format!("{}.{}.{}.{}", data[16], data[17], data[18], data[19]);

    Some(PacketInfo {
        src_ip,
        dst_ip,
        protocol,
    })
}
