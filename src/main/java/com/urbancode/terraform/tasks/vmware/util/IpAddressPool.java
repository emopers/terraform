/*******************************************************************************
 * Copyright 2012 Urbancode, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.urbancode.terraform.tasks.vmware.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class IpAddressPool implements Serializable {

    //**********************************************************************************************
    // CLASS
    //**********************************************************************************************
    private static final long serialVersionUID = 1L;

    //**********************************************************************************************
    // INSTANCE
    //**********************************************************************************************
    final private Set<Ip4> allocatedIps = new HashSet<Ip4>();
    final private Ip4 first;
    final private Ip4 last;
    private Ip4 next;

    //----------------------------------------------------------------------------------------------
    public IpAddressPool(String first, String last) {
        this(new Ip4(first), new Ip4(last));
    }

    //----------------------------------------------------------------------------------------------
    public IpAddressPool(Ip4 first, Ip4 last) {
        this.first = next = first;
        this.last = last;
        if (last.compareTo(first) < 0) {
            throw new IllegalArgumentException("Invalid range");
        }
    }

    //----------------------------------------------------------------------------------------------
    public Ip4 getFirst() {
        return first;
    }

    //----------------------------------------------------------------------------------------------
    public Ip4 getLast() {
        return last;
    }

    //----------------------------------------------------------------------------------------------
    public Ip4 getNext() {
        return next;
    }

    //----------------------------------------------------------------------------------------------
    public Set<Ip4> getAllocatedIps() {
        return Collections.unmodifiableSet(allocatedIps);
    }

    //----------------------------------------------------------------------------------------------
    public void releaseIp(Ip4 ip) {
        if (allocatedIps.contains(ip)) {
            allocatedIps.remove(ip);
        }
        findFirstUnallocated();
    }

    //----------------------------------------------------------------------------------------------
    public void releaseIp(List<Ip4> ips) {
        for (Ip4 ip : ips) {
            if (allocatedIps.contains(ip)) {
                allocatedIps.remove(ip);
            }
        }
        findFirstUnallocated();
    }

    //----------------------------------------------------------------------------------------------
    public void reserveIp(Ip4 ip)
    throws IpInUseException {
        if (allocatedIps.contains(ip)) {
            throw new IpInUseException();
        }
        allocatedIps.add(ip);
        if (ip.equals(next)) {
            incrementNext();
        }
    }

    //----------------------------------------------------------------------------------------------
    public Ip4 allocateIp() {
        Ip4 result = next;
        Ip4 startingNext = next;
        incrementNext();
        while (allocatedIps.contains(result)) {
            if (next == startingNext) {
                throw new PoolExhaustedException();
            }
            result = next;
            incrementNext();
        }
        allocatedIps.add(result);
        return result;
    }

    //----------------------------------------------------------------------------------------------
    protected void incrementNext() {
        next = next.plus(1);
        while (allocatedIps.contains(next)) {
            next = next.plus(1);
        }
        if (next.compareTo(last) > 0) {
            next = last;
        }
    }

    //----------------------------------------------------------------------------------------------
    protected void findFirstUnallocated() {
        next = first;
        while (allocatedIps.contains(next)) {
            next = next.plus(1);
        }
    }
}
