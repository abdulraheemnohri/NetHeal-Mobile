pub mod firewall;
pub mod analyzer;
pub mod healer;
pub mod engine;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jfloat};
use crate::engine::Engine;
use std::sync::Mutex;
use once_cell::sync::Lazy;

static ENGINE: Lazy<Mutex<Engine>> = Lazy::new(|| Mutex::new(Engine::new()));

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_analyze(
    mut env: JNIEnv,
    _class: JClass,
    domain: JString,
    requests: jint,
    burst: jfloat,
) -> jboolean {
    let domain_str: String = env.get_string(&domain).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();

    // Assume unknown=false for now, passing burst ratio
    let allowed = engine.process_request(&domain_str, requests as u32, false, burst as f32);

    if allowed { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_heal(
    _env: JNIEnv,
    _class: JClass,
) {
    let engine = ENGINE.lock().unwrap();
    engine.heal();
}
