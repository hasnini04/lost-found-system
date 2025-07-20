<?php
header("Content-Type: application/json");
$pdo = new PDO("mysql:host=localhost;dbname=lost_and_found;charset=utf8","root","",[
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
]);

$reporter_id = isset($_GET['reporter_id']) ? (int)$_GET['reporter_id'] : 0;
if ($reporter_id <= 0) {
    echo json_encode(["status"=>"error","message"=>"Missing reporter_id"]);
    exit;
}

$stmt = $pdo->prepare("SELECT m.*, u.name AS admin_name
                       FROM messages m
                       LEFT JOIN user u ON m.admin_id = u.id
                       WHERE m.reporter_id = :rid
                       ORDER BY m.created_at DESC");
$stmt->execute([":rid"=>$reporter_id]);

echo json_encode([
    "status" => "success",
    "data"   => $stmt->fetchAll(PDO::FETCH_ASSOC)
]);
