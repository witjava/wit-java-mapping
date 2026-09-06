;; Guest stand-in for the canonical-ABI core module of a wasi:io/streams
;; subset. The core ABI here is deliberately simplified (result structs via
;; pointer return instead of fully flat lifts) — ABI fidelity is out of scope
;; for the M2 question, which is: does the Java *declaration shape* let a
;; lowering layer work without modifying declarations?
(module
  (memory (export "memory") 1)
  (global $heap (mut i32) (i32.const 4096))
  (global $in-pos (mut i32) (i32.const 0))
  (global $in-open (mut i32) (i32.const 1))
  (global $out-open (mut i32) (i32.const 1))
  (global $out-len (mut i32) (i32.const 0))
  (global $out-buf (mut i32) (i32.const 0))
  (global $arm-fail (mut i32) (i32.const 0))
  (global $err-count (mut i32) (i32.const 0))

  (data (i32.const 256) "hello, component")
  (data (i32.const 300) "guest io failure")

  (func $alloc (param $n i32) (result i32)
    (local $p i32)
    (local.set $p (global.get $heap))
    (global.set $heap
      (i32.add (global.get $heap)
               (i32.and (i32.add (local.get $n) (i32.const 7)) (i32.const -8))))
    (local.get $p))
  (export "alloc" (func $alloc))

  (func $struct-closed (result i32)
    (local $p i32)
    (local.set $p (call $alloc (i32.const 4)))
    (i32.store (local.get $p) (i32.const 1))
    (local.get $p))

  (func (export "istream-open") (result i32) (i32.const 1))
  (func (export "ostream-open") (result i32)
    (global.set $out-open (i32.const 1))
    (global.set $out-len (i32.const 0))
    (global.set $out-buf (i32.const 0))
    (i32.const 2))

  ;; read(h: i32, len: i64) -> ptr [disc=0][len][bytes] | [disc=1] closed
  (func (export "istream-read") (param $h i32) (param $len i64) (result i32)
    (local $ptr i32) (local $n i32)
    (if (i32.eqz (global.get $in-open))
      (then (return (call $struct-closed))))
    (local.set $n (i32.wrap_i64 (local.get $len)))
    (if (i32.gt_u (local.get $n) (i32.sub (i32.const 16) (global.get $in-pos)))
      (then
        (local.set $n (i32.sub (i32.const 16) (global.get $in-pos)))))
    (local.set $ptr (call $alloc (i32.add (i32.const 8) (local.get $n))))
    (i32.store (local.get $ptr) (i32.const 0))
    (i32.store (i32.add (local.get $ptr) (i32.const 4)) (local.get $n))
    (memory.copy
      (i32.add (local.get $ptr) (i32.const 8))
      (i32.add (i32.const 256) (global.get $in-pos))
      (local.get $n))
    (global.set $in-pos (i32.add (global.get $in-pos) (local.get $n)))
    (if (i32.ge_u (global.get $in-pos) (i32.const 16))
      (then (global.set $in-open (i32.const 0))))
    (local.get $ptr))

  ;; check-write(h) -> ptr [disc=0][payload i64] | [disc=1]
  (func (export "ostream-check-write") (param $h i32) (result i32)
    (local $p i32)
    (local.set $p (call $alloc (i32.const 12)))
    (if (i32.eqz (global.get $out-open))
      (then (i32.store (local.get $p) (i32.const 1)))
      (else
        (i32.store (local.get $p) (i32.const 0))
        (i64.store (i32.add (local.get $p) (i32.const 4))
                   (i64.const 4294967296))))
    (local.get $p))

  ;; write(h, ptr, len) -> ptr [disc=0] | [disc=1] | [disc=2][error-handle]
  (func (export "ostream-write")
    (param $h i32) (param $ptr i32) (param $len i32) (result i32)
    (local $p i32) (local $eh i32)
    (if (i32.eqz (global.get $out-open))
      (then (return (call $struct-closed))))
    (if (global.get $arm-fail)
      (then
        (global.set $arm-fail (i32.const 0))
        (local.set $eh (i32.add (i32.const 100) (global.get $err-count)))
        (global.set $err-count (i32.add (global.get $err-count) (i32.const 1)))
        (local.set $p (call $alloc (i32.const 8)))
        (i32.store (local.get $p) (i32.const 2))
        (i32.store (i32.add (local.get $p) (i32.const 4)) (local.get $eh))
        (return (local.get $p))))
    (if (i32.eqz (global.get $out-buf))
      (then
        (global.set $out-buf (call $alloc (i32.const 4096)))))
    (memory.copy
      (i32.add (global.get $out-buf) (global.get $out-len))
      (local.get $ptr)
      (local.get $len))
    (global.set $out-len (i32.add (global.get $out-len) (local.get $len)))
    (local.set $p (call $alloc (i32.const 4)))
    (i32.store (local.get $p) (i32.const 0))
    (local.get $p))

  ;; flush(h) -> ptr [disc=0] | [disc=1]
  (func (export "ostream-flush") (param $h i32) (result i32)
    (local $p i32)
    (if (i32.eqz (global.get $out-open))
      (then (return (call $struct-closed))))
    (local.set $p (call $alloc (i32.const 4)))
    (i32.store (local.get $p) (i32.const 0))
    (local.get $p))

  ;; test-only: dump accumulated output [len][bytes]
  (func (export "ostream-dump") (param $h i32) (result i32)
    (local $p i32)
    (local.set $p (call $alloc (i32.add (i32.const 4) (global.get $out-len))))
    (i32.store (local.get $p) (global.get $out-len))
    (memory.copy (i32.add (local.get $p) (i32.const 4))
                 (global.get $out-buf) (global.get $out-len))
    (local.get $p))

  ;; error-debug(h) -> ptr [len][utf8]
  (func (export "error-debug") (param $h i32) (result i32)
    (local $p i32)
    (local.set $p (call $alloc (i32.const 20)))
    (i32.store (local.get $p) (i32.const 16))
    (memory.copy (i32.add (local.get $p) (i32.const 4))
                 (i32.const 300) (i32.const 16))
    (local.get $p))

  (func (export "istream-drop") (param $h i32)
    (global.set $in-open (i32.const 0)))
  (func (export "ostream-drop") (param $h i32)
    (global.set $out-open (i32.const 0)))
  (func (export "error-drop") (param $h i32))
  (func (export "arm-fail")
    (global.set $arm-fail (i32.const 1)))
)
