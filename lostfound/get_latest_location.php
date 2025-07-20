<?php
// get_latest_location.php

// Check if the temporary file exists
$filename = "latest_location.txt";

if (!file_exists($filename)) {
    http_response_code(404);
    echo "";
    exit;
}

// Read the saved address
$address = trim(file_get_contents($filename));

// Output the address
echo $address;
?>
