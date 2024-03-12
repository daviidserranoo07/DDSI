import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;
import java.util.Date;
import java.util.Vector;

public class Main {
    public static Connection conectar(String user, String password){
        Connection myConn = null;
        String url = "jdbc:oracle:thin:@oracle0.ugr.es:1521/practbd.oracle0.ugr.es";
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            myConn = DriverManager.getConnection(url, user, password);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return myConn;
    }

    public static void mostrarDatos(Connection myConn) {
        Statement myStmt = null;
        String[] tablas = {"STOCK", "PEDIDO", "DETALLE_PEDIDO"};
        for (int i = 0; i < 3; i++) {
            System.out.println("TABLA " + tablas[i]);
            try {
                Vector<String> col1 = new Vector<>();
                Vector<String> col2 = new Vector<>();
                Vector<String> col3 = new Vector<>();
                myStmt = myConn.createStatement();
                // Guarda en la ED resultado, las tablas pertenecientes a la consulta
                ResultSet resultado = myStmt.executeQuery("SELECT * FROM " + tablas[i]);
                // Recorre la ED guardando los campos de las tablas en un vector
                while (resultado.next()) {
                    if (i == 0) {
                        col1.add(resultado.getString("CPRODUCTO"));
                        col2.add(resultado.getString("CANTIDAD"));
                    }
                    if (i == 1) {
                        col1.add(resultado.getString("CPEDIDO"));
                        col2.add(resultado.getString("CCLIENTE"));
                        col3.add(resultado.getString("FECHA_PEDIDO"));
                    }
                    if (i == 2) {
                        col1.add(resultado.getString("CPEDIDO"));
                        col2.add(resultado.getString("CPRODUCTO"));
                        col3.add(resultado.getString("CANTIDAD"));
                    }
                }
                //Muestra lo encontrado en los campos de la tabla
                if (i == 0) {
                    if (col1.size() == 0) {
                        System.out.println("<tabla actualmente vacia>");
                    } else {
                        for (int j = 0; j < col1.size(); j++) {
                            System.out.println(col1.get(j) + "  " + col2.get(j));
                        }
                    }
                } else {
                    if (col1.size() == 0) {
                        System.out.println("<tabla actualmente vacia>");
                    } else {
                        for (int j = 0; j < col1.size(); j++) {
                            System.out.println(col1.get(j) + "  " + col2.get(j) + "  " + col3.get(j));
                        }
                    }
                }
            } catch (SQLException ex) {
            }
            System.out.println("-----o-----");
        }
    }

    public static String crearPedido(Connection myConn) throws SQLException{
        Statement myStm = null;
        Date todayDate=new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String fecha=sdf.format(todayDate);
        myStm = myConn.createStatement();
        boolean disponible=false;
        String cliente="0";
        while(!disponible){
            cliente = JOptionPane.showInputDialog("Introduce tu codigo de cliente");
            ResultSet resultado=myStm.executeQuery("select ccliente from pedido where ccliente="+cliente);
            disponible=true;
            while(resultado.next()){
                int ccliente=Integer.parseInt(resultado.getString("ccliente"));
                if(ccliente==Integer.parseInt(cliente)){
                    disponible=false;
                    JOptionPane.showMessageDialog(null,"Codigo de cliente no disponible");
                }
            }
        }

        myStm.execute("insert into pedido (cpedido,ccliente,fecha_pedido) values(" + cliente + "," + cliente + ",to_date('+" + fecha + "','dd/mm/yyyy'))");
        return cliente;
    }

    public static String menu(){
        String opcion=JOptionPane.showInputDialog("Selecciona la opcion deseada:\n" +
                "1.Añadir detalle de producto\n " +
                "2.Eliminar todos los detalles de producto\n" +
                "3.Cancelar Pedido\n" +
                "4.Finalizar Pedido\n");
        return opcion;
    }

    public static void main(String[] args) throws SQLException{
        Connection myConn = conectar("x7963043", "x7963043");
        Statement myStm=null;
        myStm = myConn.createStatement();
        myConn.setAutoCommit(false);
        String opcion=menu();
        String cproducto="0", cantidad="0",cpedido;
        Savepoint noPedido=null;
        Savepoint pedidoHecho=null;
        boolean add=true, menu=true, disponible=false;
        while(menu){
            switch (opcion){
                case "1":
                    String pedido="";
                    noPedido=myConn.setSavepoint();
                    pedido=crearPedido(myConn);
                    pedidoHecho=myConn.setSavepoint();
                    JOptionPane.showMessageDialog(null,"Información de las tablas mostrada por la consola");
                    mostrarDatos(myConn);
                    JOptionPane.showMessageDialog(null,"Tu codigo de pedido es: "+pedido);
                    while(add) {
                        cproducto = JOptionPane.showInputDialog("Introduce el codigo del producto deseado");
                        disponible=false;
                        while(!disponible){
                            cantidad = JOptionPane.showInputDialog("Introduce la cantidad de ese producto");
                            ResultSet resultado=myStm.executeQuery("select cantidad from stock where cproducto="+cproducto);
                            disponible=true;
                            while(resultado.next()){
                                int cantidad_stock=Integer.parseInt(resultado.getString("cantidad"));
                                if(cantidad_stock==0) {
                                    JOptionPane.showMessageDialog(null,"No queda stock de este producto");
                                }else if(cantidad_stock<Integer.parseInt(cantidad)){
                                    disponible=false;
                                    JOptionPane.showMessageDialog(null,"Cantidad no disponible");
                                }
                            }
                        }
                        try{
                            myStm.execute("insert into detalle_pedido (cpedido,cproducto,cantidad) values("+pedido+","+cproducto+","+cantidad+")");
                            myStm.execute("update stock set cantidad=cantidad - " + cantidad + " where cproducto='" + cproducto+"'");
                        }catch (Exception e){
                            e.fillInStackTrace();
                        }
                        String seguir=JOptionPane.showInputDialog("Si desea seguir añadiendo productos diga SI, en caso contrario NO");
                        if(seguir.equals("NO")) add=false;
                    }
                    JOptionPane.showMessageDialog(null,"Información de las tablas mostrada por la consola");
                    mostrarDatos(myConn);
                    opcion=menu();
                    add=true;
                    break;
                case "2":
                    myConn.rollback(pedidoHecho);
                    JOptionPane.showMessageDialog(null,"Información de las tablas mostrada por la consola");
                    mostrarDatos(myConn);
                    opcion=menu();
                    break;
                case "3":
                    myConn.rollback(noPedido);
                    JOptionPane.showMessageDialog(null,"Información de las tablas mostrada por la consola");
                    mostrarDatos(myConn);
                    opcion=menu();
                    break;
                case "4":
                    mostrarDatos(myConn);
                    myConn.commit();
                    myConn.setAutoCommit(true);
                    System.exit(0);
                    break;
                default:
            }
        }


        myConn.close();
    }
}