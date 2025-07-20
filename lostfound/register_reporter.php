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

// Get POST data from form (not JSON)
$name     = $_POST['name']     ?? '';
$matric   = $_POST['matric']   ?? '';
$faculty  = $_POST['faculty']  ?? '';
$email    = $_POST['email']    ?? '';
$password = $_POST['password'] ?? '';

// Validate
if (empty($name) || empty($matric) || empty($faculty) || empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Please fill in all fields."]);
    exit;
}

// Hash password
$hashedPassword = password_hash($password, PASSWORD_DEFAULT);

try {
    // Check if email already exists
    $stmt = $pdo->prepare("SELECT id FROM user WHERE email = ?");
    $stmt->execute([$email]);

    if ($stmt->rowCount() > 0) {
        echo json_encode(["status" => "error", "message" => "Email already registered."]);
        exit;
    }

    // Insert user
    $insert = $pdo->prepare("INSERT INTO user (name, matric, faculty, email, password, role)
                             VALUES (?, ?, ?, ?, ?, 'reporter')");
    $insert->execute([$name, $matric, $faculty, $email, $hashedPassword]);

    echo json_encode(["status" => "success", "message" => "Registration successful."]);
} catch (PDOException $e) {
    echo json_encode(["status" => "error", "message" => "Failed to register user."]);
}
?>
