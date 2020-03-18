/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.linphone.LinphoneContext;
import org.linphone.LinphoneManager;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.tools.Log;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        final int notifId = intent.getIntExtra(Compatibility.INTENT_NOTIF_ID, 0);

        if (!LinphoneContext.isReady()) {
            Log.e("[Notification Broadcast Receiver] Context not ready, aborting...");
            return;
        }

        if (intent.getAction().equals(Compatibility.INTENT_ANSWER_CALL_NOTIF_ACTION)
                || intent.getAction().equals(Compatibility.INTENT_HANGUP_CALL_NOTIF_ACTION)) {
            String remoteAddr =
                    LinphoneContext.instance()
                            .getNotificationManager()
                            .getSipUriForCallNotificationId(notifId);

            Core core = LinphoneManager.getCore();
            if (core == null) {
                Log.e("[Notification Broadcast Receiver] Couldn't get Core instance");
                return;
            }
            Call call = core.findCallFromUri(remoteAddr);
            if (call == null) {
                Log.e(
                        "[Notification Broadcast Receiver] Couldn't find call from remote address "
                                + remoteAddr);
                return;
            }

            if (intent.getAction().equals(Compatibility.INTENT_ANSWER_CALL_NOTIF_ACTION)) {
                LinphoneManager.getCallManager().acceptCall(call);
            } else {
                call.terminate();
            }
        }
    }
}
