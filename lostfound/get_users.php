<?php
header('Content-Type: application/json');

// Database connection
$host = 'localhost';
$db   = 'lost_and_found';
$user = 'root';
$pass = '';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";

try {
    $pdo = new PDO($dsn, $user, $pass);
} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database connection failed: " . $e->getMessage()
    ]);
    exit;
}

// Fetch all users
$stmt = $pdo->prepare("SELECT id, name, email, matric, faculty, role FROM user");
$stmt->execute();
$users = $stmt->fetchAll(PDO::FETCH_ASSOC);

// Send JSON response
echo json_encode([
    "status" => "success",
    "users" => $users
]);
