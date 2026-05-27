import os
import numpy as np
from pathlib import Path
import csv

# ================= ⚙️ 配置区 =================
BASE_DIR = Path('.')           # 替换为包含 1-10 子文件夹的根目录路径
TARGET_FILE = 'rank 2 5.txt'   # 若实际文件名为 rank2 25.txt，请在此修改
# =============================================

def parse_hv_column_averages(file_path):
    """读取文件中的 HV 数据块，计算每列（每个方法）的平均值"""
    hv_data = []
    in_hv = False
    
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line == 'HV':
                in_hv = True
                continue
            if in_hv:
                # 遇到分隔符、空行或非数据行，说明 HV 部分结束
                if line.startswith('---') or line == '':
                    break
                parts = line.split()
                if not parts:
                    continue
                try:
                    row = [float(x) for x in parts]
                    hv_data.append(row)
                except ValueError:
                    break  # 遇到表头或非数字行，终止读取
                    
    if not hv_data:
        return None
        
    # 计算列平均值 (axis=0 表示按列平均，即每个方法在40个case上的均值)
    return np.mean(np.array(hv_data), axis=0)

def main():
    print("🔍 开始扫描并处理数据...")
    sub_dirs = [str(i) for i in range(1, 11)]
    all_averages = []
    results_log = []

    for sd in sub_dirs:
        file_path = BASE_DIR / sd / 'FGCS' / TARGET_FILE
        if file_path.exists():
            avg_arr = parse_hv_column_averages(file_path)
            if avg_arr is not None:
                all_averages.append(avg_arr)
                results_log.append({'Dir': sd, 'Averages': avg_arr.tolist()})
                print(f"✅ {sd}/FGCS/{TARGET_FILE} -> 成功解析 {len(avg_arr)} 个方法的排名")
            else:
                print(f"⚠️ {sd}/FGCS/{TARGET_FILE} -> 未找到有效的 HV 数据块")
        else:
            print(f"❌ 路径不存在: {file_path}")

    if not all_averages:
        print("\n💡 提示：未找到任何有效数据，请检查 BASE_DIR 和文件路径配置。")
        return

    # 计算跨所有子文件夹的总平均值
    overall_avg = np.mean(np.array(all_averages), axis=0)
    n_methods = len(overall_avg)

    # ================= 🖨️ 终端打印 =================
    print("\n" + "="*70)
    print("📊 各子文件夹 HV 指标平均排名结果")
    print("="*70)
    for res in results_log:
        print(f"[文件夹 {res['Dir']}] | 均值: {np.array2string(np.array(res['Averages']), precision=3, separator=', ')}")
    print("-" * 70)
    print(f"🌍 全局总平均 | 均值: {np.array2string(overall_avg, precision=3, separator=', ')}")
    print("="*70)

    # ================= 💾 导出 CSV =================
    csv_path = BASE_DIR / 'HV_Average_Rankings.csv'
    with open(csv_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        header = ['Folder'] + [f'Method_{i+1}' for i in range(n_methods)]
        writer.writerow(header)
        
        for res in results_log:
            writer.writerow([res['Dir']] + [f"{x:.3f}" for x in res['Averages']])
            
        writer.writerow(['Overall_Average'] + [f"{x:.3f}" for x in overall_avg])
    print(f"💾 详细数据已导出至: {csv_path}")

if __name__ == '__main__':
    main()