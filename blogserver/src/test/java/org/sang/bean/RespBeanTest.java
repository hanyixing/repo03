package org.sang.bean;

import org.junit.Test;
import static org.junit.Assert.*;

public class RespBeanTest {

    @Test
    public void testDefaultConstructor() {
        RespBean respBean = new RespBean();
        assertNull(respBean.getStatus());
        assertNull(respBean.getMsg());
    }

    @Test
    public void testParameterizedConstructor() {
        RespBean respBean = new RespBean("success", "OK");
        assertEquals("success", respBean.getStatus());
        assertEquals("OK", respBean.getMsg());
    }

    @Test
    public void testSetters() {
        RespBean respBean = new RespBean();
        respBean.setStatus("error");
        respBean.setMsg("Something went wrong");
        assertEquals("error", respBean.getStatus());
        assertEquals("Something went wrong", respBean.getMsg());
    }
}
