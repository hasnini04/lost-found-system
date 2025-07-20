<?php
header("Content-Type: application/json");

// DB connection
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

// Get data from form
$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

// Validate
if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Email and password are required."]);
    exit;
}

try {
    // Get admin user
    $stmt = $pdo->prepare("SELECT * FROM user WHERE email = ? AND role = 'admin'");
    $stmt->execute([$email]);

    if ($stmt->rowCount() === 1) {
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if (password_verify($password, $user['password'])) {
            echo json_encode(["status" => "success", "message" => "Login successful."]);
        } else {
            echo json_encode(["status" => "error", "message" => "Incorrect password."]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Admin account not found."]);
    }
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Query error."]);
}
?>
