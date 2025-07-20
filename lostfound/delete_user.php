<?php
header('Content-Type: application/json');

// Database connection
$host = 'localhost';
$db   = 'lost_and_found';
$user = 'root';
$pass = '';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";
$options = [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
];

try {
    $pdo = new PDO($dsn, $user, $pass, $options);
} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Database connection failed: " . $e->getMessage()
    ]);
    exit;
}

// Get user ID from POST
$id = isset($_POST['id']) ? intval($_POST['id']) : 0;

if ($id <= 0) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid user ID"
    ]);
    exit;
}

// Delete user
$sql = "DELETE FROM user WHERE id = :id";
$stmt = $pdo->prepare($sql);
$success = $stmt->execute([':id' => $id]);

if ($success) {
    echo json_encode([
        "status" => "success",
        "message" => "User deleted successfully"
    ]);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Failed to delete user"
    ]);
}
