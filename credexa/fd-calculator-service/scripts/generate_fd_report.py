#!/usr/bin/env python3
"""
Lab L11: FD Calculator Report Generation Script
Generates CSV reports for FD calculations with detailed breakdown
"""

import csv
import json
import sys
from datetime import datetime
from pathlib import Path

def generate_fd_report(calculations_data, output_file="fd_calculation_report.csv"):
    """
    Generate CSV report from FD calculation data
    
    Args:
        calculations_data: List of FD calculation results
        output_file: Output CSV file path
    """
    # Define CSV headers
    headers = [
        "Username",
        "Principal",
        "Term",
        "BaseRate",
        "CategoryAddOn",
        "FinalRate",
        "Maturity",
        "InterestEarned",
        "Categories",
        "CalculationType",
        "GeneratedAt"
    ]
    
    # Create output directory if it doesn't exist
    output_path = Path(output_file)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Write CSV report
    with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=headers)
        writer.writeheader()
        
        for calc in calculations_data:
            writer.writerow({
                "Username": calc.get("username", "unknown"),
                "Principal": calc.get("principalAmount", 0),
                "Term": f"{calc.get('tenure', 0)}{calc.get('tenureUnit', 'M')[0]}",
                "BaseRate": calc.get("baseInterestRate", 0),
                "CategoryAddOn": calc.get("additionalInterestRate", 0),
                "FinalRate": calc.get("interestRate", 0),
                "Maturity": calc.get("maturityAmount", 0),
                "InterestEarned": calc.get("interestEarned", 0),
                "Categories": ", ".join(calc.get("customerClassifications", [])),
                "CalculationType": calc.get("calculationType", "SIMPLE"),
                "GeneratedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            })
    
    print(f"✅ Report generated successfully: {output_file}")
    print(f"📊 Total calculations: {len(calculations_data)}")
    return output_file

def read_calculation_data(input_file):
    """
    Read calculation data from JSON file
    
    Args:
        input_file: Path to JSON file containing calculation data
    
    Returns:
        List of calculation dictionaries
    """
    with open(input_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
        if isinstance(data, list):
            return data
        elif isinstance(data, dict) and 'calculations' in data:
            return data['calculations']
        else:
            return [data]

def main():
    """
    Main function - handles command line arguments
    
    Usage:
        python generate_fd_report.py <input_json_file> [output_csv_file]
    """
    if len(sys.argv) < 2:
        print("❌ Error: Missing input file")
        print("Usage: python generate_fd_report.py <input_json_file> [output_csv_file]")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else "fd_calculation_report.csv"
    
    try:
        # Read calculation data
        calculations = read_calculation_data(input_file)
        
        # Generate report
        report_path = generate_fd_report(calculations, output_file)
        
        print(f"\n📄 Report Details:")
        print(f"   Input:  {input_file}")
        print(f"   Output: {report_path}")
        print(f"   Status: SUCCESS")
        
        sys.exit(0)
        
    except FileNotFoundError:
        print(f"❌ Error: Input file not found: {input_file}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"❌ Error: Invalid JSON format in input file: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
