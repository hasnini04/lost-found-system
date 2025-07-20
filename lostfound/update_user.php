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

// Get and validate input
$id      = isset($_POST['id'])      ? intval($_POST['id']) : 0;
$name    = isset($_POST['name'])    ? trim($_POST['name']) : '';
$email   = isset($_POST['email'])   ? trim($_POST['email']) : '';
$matric  = isset($_POST['matric'])  ? trim($_POST['matric']) : null;
$faculty = isset($_POST['faculty']) ? trim($_POST['faculty']) : null;

if ($id <= 0 || $name === '' || $email === '') {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required fields"
    ]);
    exit;
}

// Prepare update statement
$sql = "UPDATE user SET name = :name, email = :email, matric = :matric, faculty = :faculty WHERE id = :id";
$stmt = $pdo->prepare($sql);
$success = $stmt->execute([
    ':name'    => $name,
    ':email'   => $email,
    ':matric'  => $matric,
    ':faculty' => $faculty,
    ':id'      => $id
]);

if ($success) {
    echo json_encode(["status" => "success", "message" => "User updated"]);
} else {
    echo json_encode(["status" => "error", "message" => "Update failed"]);
}
