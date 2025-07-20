<?php
$host = "localhost";
$user = "root";
$password = "";
$dbname = "lost_and_found";

$conn = new mysqli($host, $user, $password, $dbname);
if ($conn->connect_error) {
    die("Connection error: " . $conn->connect_error);
}

$name = "Admin Test";
$email = "admin_test@utem.edu.my";
$plainPassword = "admintest123";
$hashedPassword = password_hash($plainPassword, PASSWORD_BCRYPT);

// Only insert if email doesn't already exist
$check = $conn->prepare("SELECT * FROM user WHERE email = ?");
$check->bind_param("s", $email);
$check->execute();
$result = $check->get_result();

if ($result->num_rows === 0) {
    $sql = "INSERT INTO user (name, matric, faculty, email, password, role)
            VALUES (?, NULL, NULL, ?, ?, 'admin')";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("sss", $name, $email, $hashedPassword);

    if ($stmt->execute()) {
        echo "✅ Admin inserted successfully!";
    } else {
        echo "❌ Error: " . $stmt->error;
    }

    $stmt->close();
} else {
    echo "⚠️ Admin already exists.";
}

$check->close();
$conn->close();
?>
