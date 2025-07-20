<?php
header("Content-Type: application/json");

$host = "localhost";
$dbname = "lost_and_found";
$user = "root";
$pass = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $user, $pass);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Database connection failed."]);
    exit;
}

$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Missing email or password."]);
    exit;
}

try {
    $stmt = $pdo->prepare("SELECT * FROM user WHERE email = ? AND role = 'reporter'");
    $stmt->execute([$email]);

    if ($stmt->rowCount() === 1) {
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        if (password_verify($password, $user['password'])) {
            echo json_encode(["status" => "success", "message" => "Login successful", "user_id" => $user['id'] ]);
        } else {
            echo json_encode(["status" => "error", "message" => "Invalid password."]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Reporter not found."]);
    }
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Query error."]);
}
?>
