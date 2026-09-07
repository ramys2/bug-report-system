CREATE DATABASE IF NOT EXISTS bug_report_test;

GRANT ALL PRIVILEGES ON bug_report_test.*
TO 'bug_report'@'%';

FLUSH PRIVILEGES;