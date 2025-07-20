<?php
header("Content-Type: application/json");

// DB connection
$pdo = new PDO("mysql:host=localhost;dbname=lost_and_found;charset=utf8","root","",[
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
]);

$reporter_id = isset($_POST['reporter_id']) ? (int)$_POST['reporter_id'] : 0;
$message     = trim($_POST['message'] ?? '');
$item_id     = isset($_POST['item_id']) && $_POST['item_id'] !== '' ? (int)$_POST['item_id'] : null;
$admin_id    = isset($_POST['admin_id']) && $_POST['admin_id'] !== '' ? (int)$_POST['admin_id'] : null;

if ($reporter_id <= 0 || $message === '') {
    echo json_encode(["status"=>"error","message"=>"reporter_id or message missing"]);
    exit;
}

$stmt = $pdo->prepare("INSERT INTO messages (reporter_id, admin_id, item_id, message)
                       VALUES (:rid, :aid, :iid, :msg)");
$stmt->execute([
    ":rid" => $reporter_id,
    ":aid" => $admin_id,
    ":iid" => $item_id,
    ":msg" => $message
]);

echo json_encode(["status"=>"success","id"=>$pdo->lastInsertId()]);
