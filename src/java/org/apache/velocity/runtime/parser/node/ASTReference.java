//* Generated By:JJTree: Do not edit this line. ASTReference.java */

package org.apache.velocity.runtime.parser.node;

import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import java.lang.reflect.Method;

import org.apache.velocity.Context;
import org.apache.velocity.runtime.Runtime;
import org.apache.velocity.runtime.exception.ReferenceException;
import org.apache.velocity.runtime.parser.*;

public class ASTReference extends SimpleNode
{
    /* Reference types */
    private static final int NORMAL_REFERENCE = 1;
    private static final int FORMAL_REFERENCE = 2;
    private static final int QUIET_REFERENCE = 3;
    
    private int referenceType;
    private String nullString;
    private Object rootObject;
    private Object value;
    private String rootString;
    
    public ASTReference(int id)
    {
        super(id);
    }

    public ASTReference(Parser p, int id)
    {
        super(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    public Object init(Context context, Object data) throws Exception
    {
        rootString = getRoot();
        rootObject = getVariableValue(context, rootString);
        
        // An object has to be in the context for
        // subsequent introspection.

        if (rootObject == null) return null;
        
        Class clazz = rootObject.getClass();
        
        // All children here are either Identifier() nodes
        // or Method() nodes.
        
        int children = jjtGetNumChildren();
        for (int i = 0; i < children; i++)
            clazz = (Class) jjtGetChild(i).init(context, clazz);
    
        return data;
    }        
    
    public Object execute(Object o, Context context)
    {
        Object result = getVariableValue(context, rootString);
        
        if (result == null)
            return null;
        
        int children = jjtGetNumChildren();
        
        for (int i = 0; i < children; i++)
            result = jjtGetChild(i).execute(result,context);
    
        return result;
    }

    public boolean render(Context context, Writer writer)
        throws IOException
    {
        value = execute(null, context);
        
        if (value == null)
        {
            writer.write(NodeUtils
                .specialText(getFirstToken()) + 
                    nullString);
            
            if (referenceType != QUIET_REFERENCE)
                Runtime.error(new ReferenceException("reference", this));
        }                    
        else
        {
            writer.write(NodeUtils
                .specialText(getFirstToken()) +
                    value.toString());
        }                    
    
        return true;
    }

    public boolean evaluate(Context context)
    {
        value = execute(null, context);
        
        if (value == null)
            return false;
        else if (value instanceof Boolean)
        {
            if (((Boolean) value).booleanValue())
                return true;
            else
                return false;
        }
        else
            return true;
    }

    public Object value(Context context)
    {
        return execute(null, context);
    }

    public boolean setValue(Context context, Object value)
    {
        // The rootOfIntrospection is the object we will
        // retrieve from the Context. This is the base
        // object we will apply reflection to.
        Object result = getVariableValue(context, rootString);
        
        if (result == null)
        {
            Runtime.error(new ReferenceException("#set", this));
            return false;
        }                          
        
        // How many child nodes do we have?
        int children = jjtGetNumChildren();

        for (int i = 0; i < children - 1; i++)
        {
            result = jjtGetChild(i).execute(result, context);
            
            if (result == null)
            {
                Runtime.error(new ReferenceException("#set", this));
                return false;
            }                          
        }            

        Object[] args = { value };
        Class[] params = { value.getClass() };
        
        /*
         * This catches the last phase of setting a property
         * if we catch an exception we know that something
         * like $provider.Monkey is not a valid reference.
         * $provider may be in the context, but Monkey is
         * not a method of $provider.
         */
        try
        {
            Class c = result.getClass();
            Method m = c.getMethod("set" + jjtGetChild(children - 1).getFirstToken().image, params);
            m.invoke(result, args);
        }
        catch (Exception e)
        {
            Runtime.error(new ReferenceException("#set", this));
            return false;
        }
        
        return true;
    }

    private String getRoot()
    {
        Token t = getFirstToken();
        
        /*
         *  geirm :   changed Parser.jjt to handle $foo! 
         *  so the tree structure changed.  Leaving this stuff here
         *  for a little while in case something bad happens. :)
         *  following line was ->  if (t.image.equals("$!"))
         */

        if (t.image.startsWith("$!"))
        {
            referenceType = QUIET_REFERENCE;
            nullString = "";
            
            /*
             *  geirm : Parser.jjt change. was ->  if (t.next.image.equals("{"))
             */

            if (t.image.startsWith("$!{"))
            {
                /*
                 *  ex : $!{provider.Title} 
                 */

                /*
                 * geirm : Parser.jjt change.  Was -> return t.next.next.image;
                 */

                return t.next.image;
            }
            else
            {
                /*
                 *  ex : $!provider.Title
                 */
                
                /* 
                 *  geirm : Parser.jjt change.  Was -> return t.next.image;
                 */

                return t.image.substring(2);
            }
        }
        else if (t.image.equals("${"))
        {
            /*
             *  ex : ${provider.Title}
             */

            referenceType = FORMAL_REFERENCE;
            nullString = literal();
            return t.next.image;
        }            
        else
        {
            /*
             *  ex : $provider.Title
             */

            referenceType = NORMAL_REFERENCE;
            nullString = literal();
            return t.image.substring(1);
        }            
    }

    /**
     * Return the literal string representation
     * of a reference. Used when a reference has
     * a null value.
     *
     * For a variable reference like $foo this isn't
     * working. hmmm.
     */
    public String literal()
    {
        Token t = getFirstToken();
        StringBuffer sb = new StringBuffer(t.image);
        
        // Check to see if there are any children. If
        // there aren't then we can return with the first
        // token becasue it's a shorthand variable reference
        // like $foo.
        if (children == null && referenceType == NORMAL_REFERENCE)
            return sb.toString();
        
        while(t.next != null && t.next.last == false)
        {
            t = t.next;
            sb.append(t.image);
        }
        sb.append(getLastToken().image);
        
        return sb.toString();
    }

    public Object getVariableValue(Context context, String variable)
    {
        if (context.containsKey(variable))
            return context.get(variable);
        else
            return null;
    }
}
