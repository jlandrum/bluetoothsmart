/**
 * Copyright 2016 James Landrum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jameslandrum.bluetoothsmart.actions;

import android.support.annotation.Nullable;

import com.jameslandrum.bluetoothsmart.SmartDevice;

/**
 * Disconnects from the existing connection explicitly.
 */
public class Disconnect extends Action {
	public Disconnect() {
		super();
	}

	@Nullable
	@Override
	public ActionError execute(SmartDevice smartDevice) {
		super.execute(smartDevice);
		smartDevice.disconnect();
		return null;
	}

	@Override
	public boolean noDelay() {
		return true;
	}

	@Override
	public String toString() {
		return "Disconnect";
	}
}
