/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

#include <iostream>
#include <sys/stat.h>
#include <unistd.h>
#include "init_flags.h"
#include "utils.h"

using bluetooth::common::InitFlags;

bool is_default_bluetooth() {
  int hci_adapter = InitFlags::GetAdapterIndex();
  return hci_adapter == 0;
}
