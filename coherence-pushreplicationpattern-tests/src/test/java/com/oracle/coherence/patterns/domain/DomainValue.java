package com.oracle.coherence.patterns.domain;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

/**
 * A very simple cache value to test any serialization issues
 *
 * @author Jonathan Knight
 */
public class DomainValue implements PortableObject
{
    private String value;

    public DomainValue()
    {
    }

    public DomainValue(String value)
    {
        this.value = value;
    }

    public String getValue()
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

        DomainValue that = (DomainValue) o;

        return !(value != null ? !value.equals(that.value) : that.value != null);
    }

    @Override
    public int hashCode()
    {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString()
    {
        return "DomainValue( value='" + value + "')";
    }

    @Override
    public void readExternal(PofReader in) throws IOException
    {
        value = in.readString(0);
    }

    @Override
    public void writeExternal(PofWriter out) throws IOException
    {
        out.writeString(0, value);
    }
}
