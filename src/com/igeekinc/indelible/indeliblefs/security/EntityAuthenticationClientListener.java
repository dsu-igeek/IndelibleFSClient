/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.indelible.indeliblefs.security;

import java.util.EventListener;

public interface EntityAuthenticationClientListener extends EventListener
{
    /**
     * A security server was seen on the network or otherwise appeared.  The server may
     * not be trusted yet
     * @param addedEvent
     */
    public void entityAuthenticationServerAppeared(EntityAuthenticationServerAppearedEvent addedEvent);
    /**
     * A security server that was present on the network has disappeared
     * @param removedEvent
     */
    public void entityAuthenticationServerDisappeared(EntityAuthenticationServerDisappearedEvent removedEvent);
    
    /**
     * A security server has been added to the trusted list
     * @param trustedEvent
     */
    public void entityAuthenticationServerTrusted(EntityAuthenticationServerTrustedEvent trustedEvent);
    
    /**
     * A security server has been removed from the trusted list
     */
    public void entityAuthenticationServerUntrusted(EntityAuthenticationServerUntrustedEvent untrustedEvent);
}
