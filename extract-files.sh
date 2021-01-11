#!/bin/bash
#
# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2017-2021 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

set -e

# Load extract_utils and do some sanity checks
MY_DIR="${BASH_SOURCE%/*}"
if [[ ! -d "${MY_DIR}" ]]; then MY_DIR="${PWD}"; fi

ANDROID_ROOT="${MY_DIR}/../../.."

HELPER="${ANDROID_ROOT}/tools/extract-utils/extract_utils.sh"
if [ ! -f "${HELPER}" ]; then
    echo "Unable to find helper script at ${HELPER}"
    exit 1
fi
source "${HELPER}"

# Default to sanitizing the vendor folder before extraction
CLEAN_VENDOR=true

ONLY_COMMON=
ONLY_TARGET=
SECTION=
KANG=

while [ "${#}" -gt 0 ]; do
    case "${1}" in
        --only-common )
                ONLY_COMMON=true
                ;;
        --only-target )
                ONLY_TARGET=true
                ;;
        -n | --no-cleanup )
                CLEAN_VENDOR=false
                ;;
        -k | --kang )
                KANG="--kang"
                ;;
        -s | --section )
                SECTION="${2}"; shift
                CLEAN_VENDOR=false
                ;;
        * )
                SRC="${1}"
                ;;
    esac
    shift
done

if [ -z "${SRC}" ]; then
    SRC="adb"
fi

function blob_fixup() {
    case "${1}" in
    product/lib64/libdpmframework.so | product/lib/libdpmframework.so )
        sed -i "s/libhidltransport.so/libcutils-v29.so\x00\x00\x00/" "${2}"
    ;;
    lib64/libwfdnative.so | lib/libwfdnative.so )
        "${PATCHELF}" --remove-needed "android.hidl.base@1.0.so" "${2}"
    ;;
    product/etc/permissions/qcnvitems.xml | product/etc/permissions/vendor.qti.hardware.factory.xml | product/etc/permissions/vendor-qti-hardware-sensorscalibrate.xml )
        sed -i 's/\/system\/framework\//\/system\/product\/framework\//g' "${2}"
    ;;
    product/etc/permissions/vendor.qti.hardware.data.connection-V1.0-java.xml | product/etc/permissions/vendor.qti.hardware.data.connection-V1.1-java.xml )
        sed -i 's/xml version="2.0"/xml version="1.0"/' "${2}"
    ;;
    vendor/etc/permissions/com.fingerprints.extension.xml )
        sed -i 's/\/system\/framework\//\/vendor\/framework\//g' "${2}"
    ;;
    vendor/lib64/hw/fingerprint.goodix.so | vendor/lib/hw/fingerprint.goodix.so )
        sed -i 's|\x00goodix.fingerprint\x00|\x00fingerprint\x00\x00\x00\x00\x00\x00\x00\x00|' "${2}"
    ;;
    esac
}


if [ -z "${ONLY_TARGET}" ]; then
    # Initialize the helper for common device
    setup_vendor "${DEVICE_COMMON}" "${VENDOR}" "${ANDROID_ROOT}" true "${CLEAN_VENDOR}"

    extract "${MY_DIR}/proprietary-files.txt" "${SRC}" "${KANG}" --section "${SECTION}"
    extract "${MY_DIR}/proprietary-files-qc.txt" "${SRC}" "${KANG}" --section "${SECTION}"
    extract "${MY_DIR}/proprietary-files-qc-perf.txt" "${SRC}" "${KANG}" --section "${SECTION}"
fi

if [ -z "${ONLY_COMMON}" ] && [ -s "${MY_DIR}/../${DEVICE}/proprietary-files.txt" ]; then
    # Reinitialize the helper for device
    source "${MY_DIR}/../${DEVICE}/extract-files.sh"
    setup_vendor "${DEVICE}" "${VENDOR}" "${ANDROID_ROOT}" false "${CLEAN_VENDOR}"

    extract "${MY_DIR}/../${DEVICE}/proprietary-files.txt" "${SRC}" "${KANG}" --section "${SECTION}"
fi

COMMON_BLOB_ROOT="${LINEAGE_ROOT}/vendor/${VENDOR}/${DEVICE_COMMON}/proprietary"

"${MY_DIR}/setup-makefiles.sh"
