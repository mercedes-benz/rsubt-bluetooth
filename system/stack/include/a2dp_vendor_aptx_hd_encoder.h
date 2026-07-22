/*
 * Copyright 2016 The Android Open Source Project
 *  Copyright 2026 Mercedes Benz Group AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 *  Copyright 2026 Mercedes Benz Group AG
 * SPDX-License-Identifier: BSD-3-Clause-Clear.
 */

//
// Interface to the A2DP aptX-HD Encoder
//

#ifndef A2DP_VENDOR_APTX_HD_ENCODER_H
#define A2DP_VENDOR_APTX_HD_ENCODER_H

#include "a2dp_codec_api.h"
#include "a2dp_vendor.h"

typedef struct {
  uint64_t sleep_time_ns;
  uint32_t pcm_reads;
  uint32_t pcm_bytes_per_read;
  uint32_t aptx_hd_bytes;
  uint32_t frame_size_counter;
} tAPTX_HD_FRAMING_PARAMS;

typedef struct {
  uint64_t session_start_us;

  size_t media_read_total_expected_packets;
  size_t media_read_total_expected_reads_count;
  size_t media_read_total_expected_read_bytes;

  size_t media_read_total_dropped_packets;
  size_t media_read_total_actual_reads_count;
  size_t media_read_total_actual_read_bytes;
} a2dp_aptx_hd_encoder_stats_t;

typedef struct {
  a2dp_source_read_callback_t read_callback;
  a2dp_source_enqueue_callback_t enqueue_callback;

  bool use_SCMS_T;
  bool is_peer_edr;          // True if the peer device supports EDR
  bool peer_supports_3mbps;  // True if the peer device supports 3Mbps EDR
  uint16_t peer_mtu;         // MTU of the A2DP peer
  uint32_t timestamp;        // Timestamp for the A2DP frames

  tA2DP_ENCODER_INIT_PEER_PARAMS peer_params;
  tA2DP_FEEDING_PARAMS feeding_params;
  tAPTX_HD_FRAMING_PARAMS framing_params;
  void* aptx_hd_encoder_state;
  a2dp_aptx_hd_encoder_stats_t stats;
} tA2DP_APTX_HD_ENCODER_CB;


// Loads the A2DP aptX-HD encoder.
// Return loading codec status
tLOADING_CODEC_STATUS A2DP_VendorLoadEncoderAptxHd(void);

// Unloads the A2DP aptX-HD encoder.
void A2DP_VendorUnloadEncoderAptxHd(void);

class A2dpAptxHdEncoder:public A2dpEncoderInterface {
public:
  A2dpAptxHdEncoder(const RawAddress& peer_address) {
    peer_address_ = peer_address;
  };

  ~A2dpAptxHdEncoder() {
    encoder_cleanup();
  };

  // Initialize the A2DP aptX-HD encoder.
  // |p_peer_params| contains the A2DP peer information.
  // The current A2DP codec config is in |a2dp_codec_config|.
  // |read_callback| is the callback for reading the input audio data.
  // |enqueue_callback| is the callback for enqueueing the encoded audio data.
  void encoder_init(
      tA2DP_ENCODER_INIT_PEER_PARAMS* p_peer_params,
      A2dpCodecConfig* a2dp_codec_config,
      a2dp_source_read_callback_t read_callback,
      a2dp_source_enqueue_callback_t enqueue_callback);

  // Cleanup the A2DP aptX-HD encoder.
  void encoder_cleanup(void);

  // Reset the feeding for the A2DP aptX-HD encoder.
  void feeding_reset(void);

  // Get the A2DP aptX-HD encoded maximum frame size
  int get_effective_frame_size();

  // Flush the feeding for the A2DP aptX-HD encoder.
  void feeding_flush(void);

  // Get the A2DP aptX-HD encoder interval (in milliseconds).
  uint64_t get_encoder_interval_ms(void);

  // Prepare and send A2DP aptX-HD encoded frames.
  // |timestamp_us| is the current timestamp (in microseconds).
  void send_frames(uint64_t timestamp_us);

  void a2dp_vendor_aptx_hd_encoder_update(
    A2dpCodecConfig* a2dp_codec_config,
    bool* p_restart_input, bool* p_restart_output, bool* p_config_updated);

  tA2DP_APTX_HD_ENCODER_CB a2dp_aptx_hd_encoder_cb;

private:
  void aptx_hd_init_framing_params(
      tAPTX_HD_FRAMING_PARAMS* framing_params);
  void aptx_hd_update_framing_params(
      tAPTX_HD_FRAMING_PARAMS* framing_params);
  size_t aptx_hd_encode_24bit(tAPTX_HD_FRAMING_PARAMS* framing_params,
                                     size_t* data_out_index,
                                     uint32_t* data32_in,
                                     uint8_t* data_out);

};

typedef int (*tAPTX_HD_ENCODER_INIT)(void* state, short endian);

typedef int (*tAPTX_HD_ENCODER_ENCODE_STEREO)(void* state, void* pcmL,
                                              void* pcmR, void* buffer);

typedef int (*tAPTX_HD_ENCODER_SIZEOF_PARAMS)(void);

typedef struct {
  tAPTX_HD_ENCODER_INIT init_func;
  tAPTX_HD_ENCODER_ENCODE_STEREO encode_stereo_func;
  tAPTX_HD_ENCODER_SIZEOF_PARAMS sizeof_params_func;
} tAPTX_HD_API;

// Filled the |external_api| with the ptr to the codec api
// return true if the codec is loaded
// This is for test purpose and ensure we are testing the api in real life
// condition
bool A2DP_VendorCopyAptxHdApi(tAPTX_HD_API& external_api);

#endif  // A2DP_VENDOR_APTX_HD_ENCODER_H
