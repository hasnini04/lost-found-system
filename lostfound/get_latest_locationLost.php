<?php


header('Content-Type: text/plain; charset=UTF-8');

// Where we keep the last chosen location for LOST items
$file = __DIR__ . '/latest_location_lost.txt';

if (is_file($file)) {
    // Read once; trim whitespace & newlines
    $location = trim(file_get_contents($file));
    if ($location !== '') {
        echo $location;
        exit;
    }
}

// No location yet (or file empty)
echo '';
