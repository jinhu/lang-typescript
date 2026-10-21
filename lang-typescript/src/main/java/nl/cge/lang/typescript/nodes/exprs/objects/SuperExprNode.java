package nl.cge.lang.typescript.nodes.exprs.objects;

import nl.cge.lang.typescript.exceptions.TypeScriptException;
import nl.cge.lang.typescript.nodes.exprs.TypeScriptExprNode;
import nl.cge.lang.typescript.runtime.ClassPrototypeObject;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * The Node implementing the 'super' expression.
 * Identical to the class with the same name from part 16.
 */
public final class SuperExprNode extends TypeScriptExprNode {
    private final ClassPrototypeObject classPrototype;

    @SuppressWarnings("FieldMayBeFinal")
    @Child
    private ThisExprNode thisExprNode;

    @Child
    private InteropLibrary interopLibrary;

    public SuperExprNode(ClassPrototypeObject classPrototype) {
        this.classPrototype = classPrototype;
        this.thisExprNode = new ThisExprNode();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // executeGeneric() simply returns 'this'
        // (that will be the method that property access Nodes use to establish the method call receiver,
        // in their evaluateAsReceiver() methods)
        return this.thisExprNode.executeGeneric(frame);
    }

    @Override
    public Object evaluateAsReceiver(VirtualFrame frame) {
        // receiver should be 'this' for a 'super()' call
        return this.executeGeneric(frame);
    }

    @Override
    public Object evaluateAsFunction(VirtualFrame frame, Object receiver) {
        if (this.interopLibrary == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.interopLibrary = this.insert(
                    InteropLibrary.getFactory().createDispatched(1));
        }
        try {
            return this.interopLibrary.readMember(
                    this.classPrototype.prototype, "constructor");
        } catch (UnknownIdentifierException e) {
            return this.currentLanguageContext().emptyFunction;
        } catch (UnsupportedMessageException e) {
            throw new TypeScriptException(this, e.getMessage());
        }
    }

    public Object readParentPrototype() {
        // this method is called from the property access Nodes
        // to find the parent prototype
        return this.classPrototype.prototype;
    }
}
