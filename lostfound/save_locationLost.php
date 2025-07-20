<?php
header('Content-Type: application/json; charset=UTF-8');

$address = isset($_POST['address']) ? trim($_POST['address']) : '';
if ($address === '') {
    echo json_encode(["status" => "error", "message" => "No address"]);
    exit;
}

$file = __DIR__ . '/latest_location_lost.txt';
if (file_put_contents($file, $address) === false) {
    echo json_encode(["status" => "error", "message" => "Write failed"]);
    exit;
}

echo json_encode(["status" => "success", "address" => $address]);
