<?php
header('Content-Type: application/json');

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$host = 'localhost';
$db   = 'lost_and_found';
$user = 'root';
$pass = '';

try {
    $conn = new mysqli($host, $user, $pass, $db);
    $conn->set_charset('utf8mb4');
} catch (Throwable $e) {
    echo json_encode(["status"=>"error","message"=>"DB connection failed: ".$e->getMessage()]);
    exit;
}

// Inputs ------------------------------------------------------
$found_item_id = isset($_POST['found_item_id']) ? (int)$_POST['found_item_id'] : 0;
$reporter_id   = isset($_POST['reporter_id'])   ? (int)$_POST['reporter_id']   : 0;

if ($found_item_id <= 0 || $reporter_id <= 0) {
    echo json_encode(["status"=>"error","message"=>"Missing found_item_id or reporter_id"]);
    exit;
}

$conn->begin_transaction();

try {
    // 1) Mark the found item itself as claimed
    $stmt1 = $conn->prepare("UPDATE `found_item` SET `status`='claimed' WHERE `id`=?");
    $stmt1->bind_param("i", $found_item_id);
    $stmt1->execute();
    $affected1 = $stmt1->affected_rows;
    $stmt1->close();

    // 2) Add a timestamp note to THIS REPORTER'S claim row
    $stmt2 = $conn->prepare("
        UPDATE `claim`
        SET `pickup_note` = CONCAT(IFNULL(`pickup_note`,''), 
                                   CASE WHEN `pickup_note` IS NULL OR `pickup_note`='' THEN '' ELSE ' ' END,
                                   '[CLAIMED ', NOW(), ']')
        WHERE `reporter_id`=? AND `found_item_id`=?");
    $stmt2->bind_param("ii", $reporter_id, $found_item_id);
    $stmt2->execute();
    $affected2 = $stmt2->affected_rows;
    $stmt2->close();

    $conn->commit();

    echo json_encode([
        "status"  => "success",
        "message" => "Item marked as claimed.",
        "found_item_rows_updated" => $affected1,
        "claim_rows_updated"      => $affected2
    ]);
} catch (Throwable $ex) {
    $conn->rollback();
    echo json_encode(["status"=>"error","message"=>"Transaction failed: ".$ex->getMessage()]);
}

$conn->close();
