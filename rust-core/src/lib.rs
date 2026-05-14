use jni::objects::{JClass, JString, JByteArray};
use jni::sys::{jboolean, jint, jlong, jbyteArray};
use jni::JNIEnv;
use std::sync::Mutex;
use once_cell::sync::Lazy;

mod engine;
mod firewall;
mod analyzer;
mod healer;
mod packet;

use crate::engine::Engine;

static ENGINE: Lazy<Mutex<Engine>> = Lazy::new(|| Mutex::new(Engine::new()));

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_analyze(
    mut env: JNIEnv,
    _class: JClass,
    domain: JString,
    _requests: jint,
) -> jboolean {
    let domain: String = env.get_string(&domain).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    if engine.handle_packet(domain.as_bytes(), None) { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_handlePacket(
    env: JNIEnv,
    _class: JClass,
    packet: jbyteArray,
) -> jboolean {
    let packet_array = unsafe { JByteArray::from_raw(packet) };
    let data = env.convert_byte_array(packet_array).unwrap_or_default();
    let mut engine = ENGINE.lock().unwrap();
    if engine.handle_packet(&data, None) { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setAppRule(
    mut env: JNIEnv,
    _class: JClass,
    app_id: JString,
    state: jint,
) {
    let app_id: String = env.get_string(&app_id).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_app_rule(&app_id, state as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addWhitelist(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
) {
    let ip: String = env.get_string(&ip).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_whitelist(&ip);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeWhitelist(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
) {
    let ip: String = env.get_string(&ip).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_whitelist(&ip);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addBlacklist(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
) {
    let ip: String = env.get_string(&ip).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_blacklist(&ip);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeBlacklist(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
) {
    let ip: String = env.get_string(&ip).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_blacklist(&ip);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_heal(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.heal();
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getSecurityScore(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    let engine = ENGINE.lock().unwrap();
    engine.check_health() as jint
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setSecurityLevel(
    _env: JNIEnv,
    _class: JClass,
    level: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_security_level(level as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getBlockedCount(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let engine = ENGINE.lock().unwrap();
    engine.get_blocked_count() as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getScannedCount(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let engine = ENGINE.lock().unwrap();
    engine.get_scanned_count() as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_resetStats(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.reset_stats();
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getAnalytics(
    env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    let engine = ENGINE.lock().unwrap();
    let analytics = engine.get_analytics_json();
    let array = env.byte_array_from_slice(analytics.as_bytes()).unwrap();
    array.as_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_runDiagnostics(
    env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    let engine = ENGINE.lock().unwrap();
    let diag = engine.run_diagnostics();
    let array = env.byte_array_from_slice(diag.as_bytes()).unwrap();
    array.as_raw()
}
