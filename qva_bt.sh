###############################################################################
#
# Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
# SPDX-License-Identifier: BSD-3-Clause-Clear
#
###############################################################################

###############################################################################
#
# File: qva_bt.sh
#
# Description : Utility script for QVA Bluetooth in Android
#
# Version : 1.0
#
###############################################################################

#!/bin/bash
set -e

android_root=$1

# AOSP Bluetooth source dir
aosp_bt_dir="${android_root}/packages/modules/Bluetooth"

# QVA Bluetooth source dir
qva_bt_dir="${android_root}/vendor/qcom/opensource/commonsys/packages/modules/Bluetooth"

# QVA Bluetooth source file
qva_bt_source=${qva_bt_dir}/qva_bt_source.txt

echo_info()
{
    echo "$1" > /dev/null
}

exit_error()
{
    echo_info "error: $1"
    exit 1
}

update_file()
{
    src_file=$1
    dst_file=$2

    if [ -e ${src_file} ]; then
        if [ -e ${dst_file} ]; then
            [[ `diff ${src_file} ${dst_file}` ]] && (cp -f ${src_file} ${dst_file}) || (echo_info "same_file")
        else
            echo_info "dst_file not exist, cp into ${dst_file}"
            cp -f ${src_file} ${dst_file}
        fi
    else
        echo_info "src_file ${src_file} not exist"
    fi
}

if [ ! -d ${qva_bt_dir} ]; then
    exit_error "dir ${qva_bt_dir} not exist"
fi

if [ ! -f ${qva_bt_source} ]; then
    exit_error "file ${qva_bt_source} not exist"
fi

cat ${qva_bt_source} | while read src
do
    if [ "${src}" ]; then
        if [[ ${src:0:1} = "#" || ${src:0:2} = "//" ]]; then
            continue
        fi
        echo_info "${src}"
        if [[ ${src} =~ "Android.bp" ]]; then
            src_file="${qva_bt_dir}/${src}.disable"
        else
            src_file="${qva_bt_dir}/${src}"
        fi
        dst_file="${aosp_bt_dir}/${src}"
        update_file ${src_file} ${dst_file}
    fi
done
echo_info "updated qva_bt into aosp"
