package sintactico;

import global.tabla.ControladorTS;
import global.tabla.TablaSimbolos;
import global.token.Identificador;
import global.token.Token;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import semantico.Atributo;
import lexico.AnalizadorLexico;

public class AnalizadorSintactico {

	private Stack<String> pilaEstados;
	private Stack<Atributo> pilaSimbolos;
	private Token tokenEntrada;
	private AnalizadorLexico anLex;
	private ArrayList<String> tAccion;
	private ArrayList<String> tGoTo;
	private HashMap<String,HashMap<String,String>> tablaAccion;
	private HashMap<String,HashMap<String,String>> tablaGoTo;
	private ArrayList<Regla> listaReglas;
	private ArrayList<Integer> parse;


	/**<i><b>AnalizadorSintactico()</b></i>
	 * <br>
	 * <br>
	 * Constructor del Analizador Sintactico
	 * <br>
	 * <br>
	 * Recibe como parametros el analizador lexico y
	 * la ubicacion de los ficheros de la tabla de Accion y de GoTo
	 * @param anLex
	 * @param ficheroAccion
	 * @param ficheroGoTo
	 */
	@SuppressWarnings("unchecked")
	public AnalizadorSintactico(AnalizadorLexico anLex, String ficheroAccion, String ficheroGoTo, String ficheroReglas){//Constructor
		this.anLex = anLex;
		this.tokenEntrada = anLex.leerToken();
		this.tablaAccion = new HashMap<String,HashMap<String,String>>();
		this.tablaGoTo = new HashMap<String,HashMap<String,String>>();

		//Inicializar pila de estados
		pilaEstados = new Stack<String>();
		pilaEstados.push("0");
		
		//Inicializar pila de simbolos
		pilaSimbolos = new Stack<Atributo>();

		//Inicializar parse
		parse = new ArrayList<Integer>();

		//Leer Tabla Accion
		try {
			ObjectInputStream oisAccion = new ObjectInputStream(new FileInputStream(ficheroAccion));
			tAccion = (ArrayList<String>)oisAccion.readObject();
			oisAccion.close();
			cargarTablaAccion();
		} catch (Exception e) {
			System.out.println("Error al leer el fichero: "+ficheroAccion);
			e.printStackTrace();
		}

		//Leer Tabla GoTo
		try {
			ObjectInputStream oisGoTo = new ObjectInputStream(new FileInputStream(ficheroGoTo));
			tGoTo = (ArrayList<String>)oisGoTo.readObject();
			cargarTablaGoTo();
			oisGoTo.close();
		} catch (Exception e) {
			System.out.println("Error al leer el fichero: "+ficheroGoTo);
			e.printStackTrace();
		}

		//Leer Fichero Reglas
		listaReglas = new ArrayList<Regla>();
		cargarListaReglas(ficheroReglas);

	}//Constructor

	/**
	 * <i><b>cargarTablaAccion()</b></i>
	 * <br>
	 * <br>
	 * Método que carga el contenido del fichero Decision.dat en un Hashmap
	 */
	public void cargarTablaAccion(){
		String estado = "";
		String accion = "";
		String token = "";
		Iterator<String> it = this.tAccion.iterator();
		while (it.hasNext()){
			estado = it.next();
			token = it.next();
			accion = it.next();
			if(tablaAccion.get(token) == null){
				tablaAccion.put(token, new HashMap<String,String>());
			}
			this.tablaAccion.get(token).put(estado, accion);
		}
	}

	/**
	 * <i><b>cargarTablaGoTo()</b></i>
	 * <br>
	 * <br>
	 * Método que carga el contenido del fichero GoTo.dat en un Hashmap
	 */
	public void cargarTablaGoTo(){
		String estado = "";
		String accion = "";
		String noterminal = "";
		Iterator<String> it = this.tGoTo.iterator();
		while (it.hasNext()){
			estado = it.next();
			noterminal = it.next();
			accion = it.next();
			if(tablaGoTo.get(noterminal) == null){
				tablaGoTo.put(noterminal, new HashMap<String,String>());
			}
			this.tablaGoTo.get(noterminal).put(estado, accion);
		}
	}

	/** 
	 * <i><b>buscarTabla</b></i
	 * <br>
	 * <br>
	 * Metodo auxiliar para buscar elementos en las tablas de Accion o GoTo
	 * <br>
	 * Recibe como parametros:
	 * <br>
	 * 	- la fila a buscar <elemento1> (que simboliza el estado en ambas tablas)
	 * <br>
	 * 	- la columna a buscar <elemento2> (que simboliza el token en la tabla de Accion o el no terminal en la tabla de GoTo)
	 * <br>
	 * 	- la tabla en la que se debe buscar
	 * <br>
	 * @param estado
	 * @param columna
	 * @param tabla
	 * @return Devuelve el elemento correspondiente de la tabla si existe y un String vacio en otro caso
	 */
	private String buscarTabla(String estado, String columna, HashMap<String,HashMap<String,String>> tabla){//buscarTabla
		if(tabla.get(columna) != null && tabla.get(columna).get(estado) != null){
			return tabla.get(columna).get(estado);
		}
		else
			return null;
	}//buscarTabla

	/** 
	 * <i><b>analizar()</b></i>
	 * <br>
	 * <br>
	 * Metodo que simula las iteraciones del automata del analizador sintactico
	 * <br>
	 * <br>
	 * Recibe como parametro el token correspondiente de la sentencia que se esta analizando
	 * @return 1 continuar, 0 aceptar, -1 error
	 */
	public int analizar(){
		int resultado = 1; //En proceso
		String estado = pilaEstados.peek();
//		System.out.println("Cima de la pila: " + estado);
//		System.out.println("Token: "+ tokenEntrada.aString());
		String accion = buscarTabla(estado,tokenEntrada.tipo(),tablaAccion);
		if(accion != null){
//			System.out.println("Accion: "+ accion);
			if(accion.substring(0,1).equals("d")){//Desplazar
				String tipo = "-";
				if(tokenEntrada.tipo().equals("id")){
					String lexema = ControladorTS.getLexema(((Identificador) tokenEntrada).getPos(), ((Identificador) tokenEntrada).getTabla());
					tipo=ControladorTS.buscaTipoTS(lexema);
					pilaSimbolos.push(new Atributo(tokenEntrada.tipo(), tipo, lexema));
				}
				else {
					pilaSimbolos.push(new Atributo(tokenEntrada.tipo(), tipo));
				}
				pilaEstados.push(accion.substring(1,accion.length()).trim());
				tokenEntrada = anLex.leerToken();
				
				/* Acciones semanticas */
				if(tokenEntrada.tipo().equals("function")){
					ControladorTS.flagDeclaracion();
					ControladorTS.flagFunction();
				}
				if(tokenEntrada.tipo().equals("var")){
					ControladorTS.flagDeclaracion();
					ControladorTS.flagVar();
				}
				
			}
			else if(accion.substring(0,1).equals("r")){//Reducir
				int numRegla = Integer.valueOf(accion.substring(1,accion.length()).trim());
				parse.add(numRegla);
				Regla regla = listaReglas.get(numRegla-1);
				
				if(pilaEstados.size() < regla.nElementosDer ||
						pilaSimbolos.size() < regla.nElementosDer){
					resultado = -1;
				}
				else{
					String tipo = Regla.ejecutarAccion(numRegla, pilaSimbolos);
					for(int i=0; i<regla.nElementosDer; i++){
						pilaEstados.pop();
						pilaSimbolos.pop();
					}
					estado = buscarTabla(pilaEstados.peek(), regla.parteIzq, tablaGoTo);
					pilaEstados.push(estado);
					pilaSimbolos.push(new Atributo(regla.parteIzq,tipo));
//					System.out.println("GOTO: " + estado);
					
				}
				
			}
			else if(accion.substring(0,1).equals("A")){//Aceptar
				resultado = 0;
				Regla.ejecutarAccion(1, pilaSimbolos);
				parse.add(1);
			}
		}		
		else{
			//Error Sintactico
			System.out.println("Error sintáctico en la línea: "+ this.anLex.getLineaActual() +
					" columna:"+this.anLex.getColumnaActual());
			resultado = -1;
		}
		return resultado;
	}//analizar

	/**
	 * <b><i>cargarListaReglas(String ruta)</i></b>
	 * <br>
	 * <br>
	 * <code>public void cargarListaReglas(String ruta)</code>
	 * <br>
	 * <br>
	 * Carga la lista de reglas en un array list
	 */
	public void cargarListaReglas(String ruta){
		try {
			FileInputStream ficheroReglas = new FileInputStream(ruta);
			BufferedReader br = new BufferedReader(new InputStreamReader(ficheroReglas));
			String line;
			String[] linea;
			int i = 1;
			while ((line = br.readLine()) != null) {
				linea = line.split(",", 2);
				listaReglas.add(new Regla(i,linea[0].trim(),Integer.parseInt(linea[1].trim())));
				i++;
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//cargarListaReglas

	/**
	 * <b><i>getParse()</i></b>
	 * <br>
	 * <br>
	 * <code>public ArrayList<Integer> getPârse()</code>
	 * <br>
	 * <br>
	 * Devuelve la lista de reglas aplicadas en el analisis
	 * @return Un arraylist con las reglas utilizadas en el analisis
	 */
	public ArrayList<Integer> getParse() {
		return parse;
	}

}
