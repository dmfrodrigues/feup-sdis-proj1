#!/bin/bash

set -e

test/test_backup_3.sh
test/test_backup_4.sh

test/test_delete_1.sh
test/test_delete_2.sh
test/test_delete_3.sh
test/test_delete_4.sh

test/test_restore_6.sh
test/test_restore_7.sh
test/test_restore_8.sh
test/test_restore_9.sh
test/test_restore_10.sh

test/test_status.sh

test/test_backup_1.sh
test/test_backup_2.sh

test/test_reclaim.sh

test/test_restore_1.sh
test/test_restore_2.sh
test/test_restore_3.sh
test/test_restore_4.sh
test/test_restore_5.sh
