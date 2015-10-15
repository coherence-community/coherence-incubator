package com.oracle.coherence.patterns.domain;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

/**
 * A very simple cache key to test any serialization issues
 *
 * @author Jonathan Knight
 */
public class DomainKey implements PortableObject
{
    private int value;

    public DomainKey()
    {
    }

    public DomainKey(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        DomainKey domainKey = (DomainKey) o;

        return value == domainKey.value;
    }

    @Override
    public int hashCode()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "DomainKey( value=" + value + ')';
    }

    @Override
    public void readExternal(PofReader in) throws IOException
    {
    value = in.readInt(0);
    }

    @Override
    public void writeExternal(PofWriter out) throws IOException
    {
    out.writeInt(0, value);
    }
}
