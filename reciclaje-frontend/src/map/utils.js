// src/map/utils.js
/**
 * Recibe un objeto contenedor con propiedades:
 *   - bloqueo (boolean)
 *   - lleno (boolean)
 *   - carga_actual (número)
 *   - capacidad_maxima (número)
 * Devuelve un string con el color hexadecimal correspondiente.
 */
export function colorPorEstado(c) {
  if (c.bloqueo) return "#6b7280";            // gris
  if (c.lleno)   return "#ef4444";            // rojo
  if (c.carga_actual >= 0.75 * c.capacidad_maxima) return "#f59e0b"; // naranja
  return "#10b981";                           // verde
}
